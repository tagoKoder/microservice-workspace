package securitygrpc

import (
	"context"
	"crypto/sha256"
	"encoding/hex"
	"log"
	"strings"
	"time"

	vptypes "github.com/aws/aws-sdk-go-v2/service/verifiedpermissions/types"
	"github.com/google/uuid"
	"github.com/tagoKoder/ledger/internal/domain/port/out"
	"github.com/tagoKoder/ledger/internal/infra/security/avp"
	"github.com/tagoKoder/ledger/internal/infra/security/authz"
	authctx "github.com/tagoKoder/ledger/internal/infra/security/context"
	jwtvalidator "github.com/tagoKoder/ledger/internal/infra/security/jwt"
	"google.golang.org/grpc"
	"google.golang.org/grpc/metadata"
	"google.golang.org/grpc/status"
	"google.golang.org/grpc/codes"
)

type Interceptor struct {
	jwt   *jwtvalidator.Validator
	avp   *avp.Client
	acts  *authz.ActionResolver
	res   *authz.ResourceResolver
	audit out.AuditPort

	hashSaltIP string
	hashSaltUA string
}

func NewAuthzInterceptor(
	jwtv *jwtvalidator.Validator,
	avpc *avp.Client,
	ar *authz.ActionResolver,
	rr *authz.ResourceResolver,
	audit out.AuditPort,
	hashSaltIP, hashSaltUA string,
) *Interceptor {
	return &Interceptor{
		jwt: jwtv, avp: avpc, acts: ar, res: rr, audit: audit,
		hashSaltIP: hashSaltIP, hashSaltUA: hashSaltUA,
	}
}

func (i *Interceptor) Unary() grpc.UnaryServerInterceptor {
	return func(ctx context.Context, req any, info *grpc.UnaryServerInfo, handler grpc.UnaryHandler) (any, error) {
		// 0) correlation-id (si no viene, generar)
		cid := readIncoming(ctx, "x-correlation-id")
		if cid == "" {
			cid = uuid.NewString()
		}
		ctx = authctx.WithCorrelationID(ctx, cid)

		// devolver correlation-id en trailer para trazabilidad
		_ = grpc.SetTrailer(ctx, metadata.Pairs("x-correlation-id", cid))

		// 1) extraer bearer token del metadata
		authzHdr := readIncoming(ctx, "authorization")
		claims, err := i.jwt.ValidateBearer(ctx, authzHdr)
		if err != nil {
			i.bestEffortAudit(ctx, info.FullMethod, "DENY", "authn_failed", time.Now().UTC(), map[string]any{
				"reason": err.Error(),
			})
			return nil, status.Error(codes.Unauthenticated, "unauthenticated")
		}
		ctx = authctx.WithAccessToken(ctx, bearerValue(authzHdr))
		ctx = authctx.WithActor(ctx, authctx.Actor{
			Subject:    claims.Subject,
			CustomerID: claims.CustomerID,
			Roles:      claims.Roles,
			MFA:        claims.MFA,
		})

		// 2) request context (hash ip/ua sin PII cruda)
		ip := readIncoming(ctx, "x-forwarded-for")
		ua := readIncoming(ctx, "user-agent")
		ctx = authctx.WithIPHash(ctx, shaHex(i.hashSaltIPip))
		ctx = authctx.WithUAHash(ctx, shaHex(i.hashSaltUAua))

		// 3) action  route_template (obligatorio)
		act := i.acts.Resolve(info.FullMethod)
		ctx = authctx.WithActionID(ctx, act.ID)
		ctx = authctx.WithRouteTemplate(ctx, act.RouteTemplate)
		
		// IdempotencyKey (si existe en request)
		if info.FullMethod == "/bank.ledgerpayments.v1.PaymentsService/PostPayment" {
			if k := extractStringField(req, "IdempotencyKey"); k != "" {
				ctx = authctx.WithIdempotencyKey(ctx, k)
			} else if k := extractStringField(req, "idempotency_key"); k != "" {
				ctx = authctx.WithIdempotencyKey(ctx, k)
			}
		}

		// 4) resource
		res := i.res.Resolve(ctx, info.FullMethod, req)

		// 5) construir context AVP mínimo (Cedar JSON map)
		avpCtx := map[string]vptypes.AttributeValue{
			"correlation_id": &vptypes.AttributeValueMemberString{Value: cid},
			"channel":        &vptypes.AttributeValueMemberString{Value: "web"},
			"ip_hash":        &vptypes.AttributeValueMemberString{Value: authctx.IPHash(ctx)},
			"ua_hash":        &vptypes.AttributeValueMemberString{Value: authctx.UAHash(ctx)},
			"route_template": &vptypes.AttributeValueMemberString{Value: act.RouteTemplate},
		}

		if actor := authctx.ActorFrom(ctx); actor.CustomerID != "" {
			avpCtx["customer_id"] = &vptypes.AttributeValueMemberString{Value: actor.CustomerID}
		}
		if res.OwnerCustomerID != "" {
			avpCtx["owner_customer_id"] = &vptypes.AttributeValueMemberString{Value: res.OwnerCustomerID}
		}

		// 6) AVP IsAuthorizedWithToken (decision solo si AVP)
		dec, err := i.avp.AuthorizeWithToken(ctx, authctx.AccessToken(ctx), act, res, avpCtx)
		if err != nil {
			i.bestEffortAudit(ctx, info.FullMethod, "DENY", "avp_error", time.Now().UTC(), map[string]any{
				"error": err.Error(),
			})
			return nil, status.Error(codes.Internal, "authorization error")
		}

		ctx = authctx.WithAuthzDecision(ctx, string(dec.Decision))
		if dec.PolicyID != "" {
			ctx = authctx.WithAuthzPolicyID(ctx, dec.PolicyID)
		}

		if dec.Decision != avp.DecisionAllow {
			i.bestEffortAudit(ctx, info.FullMethod, "DENY", "not_authorized", time.Now().UTC(), map[string]any{
				"action":  act.ID,
				"resource": map[string]any{"type": res.Type, "id": res.ID},
			})
			return nil, status.Error(codes.PermissionDenied, "forbidden")
		}

		// 7) Ejecutar handler
		resp, hErr := handler(ctx, req)

		// 8) Audit best-effort: siempre DENY y acciones críticas; aquí audit de allow opcional
		// Puedes ajustar catálogo de "críticas" según ASVS.
		if isCriticalAction(act.ID) {
			code := "ok"
			if hErr != nil {
				code = "handler_error"
			}
			i.bestEffortAudit(ctx, info.FullMethod, "ALLOW", code, time.Now().UTC(), map[string]any{
				"action": act.ID,
			})
		}

		return resp, hErr
	}
}

func isCriticalAction(actionID string) bool {
	switch actionID {
	case "ledger.payments.post", "ledger.accounts.credit", "ledger.journals.manual.create":
		return true
	default:
		return false
	}
}

func (i *Interceptor) bestEffortAudit(ctx context.Context, fullMethod, decision, outcome string, at time.Time, extra map[string]any) {
	if i.audit == nil {
		return
	}
	actor := authctx.ActorFrom(ctx)
	_ = i.audit.Record(ctx,
		"AUTHZ",
		"grpc_method",
		fullMethod,
		actor.Subject,
		at,
		map[string]any{
			"decision":       decision,
			"outcome":        outcome,
			"correlation_id": authctx.CorrelationID(ctx),
			"route_template": authctx.RouteTemplate(ctx),
			"action_id":      authctx.ActionID(ctx),
			"policy_id":      authctx.AuthzPolicyID(ctx),
			"ip_hash":        authctx.IPHash(ctx),
			"ua_hash":        authctx.UAHash(ctx),
			"extra":          extra,
		},
	)
}

func readIncoming(ctx context.Context, key string) string {
	md, ok := metadata.FromIncomingContext(ctx)
	if !ok {
		return ""
	}
	vs := md.Get(strings.ToLower(key))
	if len(vs) == 0 {
		return ""
	}
	return vs[0]
}

func bearerValue(header string) string {
	h := strings.TrimSpace(header)
	if strings.HasPrefix(strings.ToLower(h), "bearer ") {
		return strings.TrimSpace(h[len("bearer "):])
	}
	return ""
}

func shaHex(s string) string {
	sum := sha256.Sum256([]byte(s))
	return hex.EncodeToString(sum[:])
}

// fallback local logging si quisieras (no PII)
func logFallback(msg string, fields map[string]any) {
	_ = fields
	log.Println(msg)
}
