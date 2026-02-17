// bff/internal/api/rest/middleware/auth_session.go

package middleware

import (
	"context"
	"log"
	"net/http"

	"github.com/tagoKoder/bff/internal/client/ports"
	"github.com/tagoKoder/bff/internal/config"
	"github.com/tagoKoder/bff/internal/security"
	"github.com/tagoKoder/bff/internal/util"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

const (
	CtxSubject   ctxKey = "subject"
	CtxRoles     ctxKey = "roles"
	CtxSession   ctxKey = "session_id"
	CtxCustomer  ctxKey = "customer_id"
	CtxUserState ctxKey = "user_status"
	CtxIdentity  ctxKey = "identity_id"
)

type AuthDeps struct {
	Config   config.Config
	Identity ports.IdentityPort
	Cookies  *security.CookieManager
	//TokenCache security.TokenCache
}

func AuthSession(deps AuthDeps, oas *OpenAPISecurity) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			ri, ok := oas.Find(r)
			if !ok || !RequiresScheme(ri.Security, "cookieAuth") {
				next.ServeHTTP(w, r)
				return
			}

			sid, ok := deps.Cookies.ReadSessionID(r)
			if !ok || sid == "" {
				log.Printf("missing sid cookie: cookieHeader=%q expectedName=%s host=%s", r.Header.Get("Cookie"), deps.Cookies.Name(), r.Host)
				w.WriteHeader(http.StatusUnauthorized)
				return
			}

			ctx := r.Context()
			ctx = security.WithSessionID(ctx, sid)

			ip := util.GetClientIP(r)
			ua := r.UserAgent()

			info, newSid, accessTok, err := getSessionInfoWithRefresh(ctx, deps, sid, ip, ua, w)
			if err != nil {
				// getSessionInfoWithRefresh ya escribió la respuesta (401/503/etc)
				return
			}
			ctx = security.WithSessionID(ctx, newSid)
			ctx = security.WithAccessToken(ctx, accessTok)

			// si refrescó, actualiza ctx con el nuevo sid
			if newSid != "" && newSid != sid {
				ctx = security.WithSessionID(ctx, newSid)
				sid = newSid
			}

			// control de estado de usuario
			if info.UserStatus != "" && info.UserStatus != "ACTIVE" {
				w.WriteHeader(http.StatusForbidden)
				return
			}

			ctx = context.WithValue(ctx, CtxSubject, info.SubjectIDOidc)
			ctx = context.WithValue(ctx, CtxRoles, info.User.Roles)
			ctx = context.WithValue(ctx, CtxSession, sid)
			ctx = context.WithValue(ctx, CtxCustomer, info.CustomerID)
			ctx = context.WithValue(ctx, CtxUserState, info.UserStatus)
			ctx = context.WithValue(ctx, CtxIdentity, info.IdentityID)
			ctx = context.WithValue(ctx, ctxKey("route_template"), ri.RouteTemplate)

			next.ServeHTTP(w, r.WithContext(ctx))
		})
	}
}

func getSessionInfoWithRefresh(
	ctx context.Context,
	deps AuthDeps,
	sid string,
	ip string,
	ua string,
	w http.ResponseWriter,
) (ports.GetSessionInfoOutput, string, string, error) {

	const skewSeconds int64 = 30

	// 1) intento normal: GetSessionInfo
	info, err := deps.Identity.GetSessionInfo(ctx, ports.GetSessionInfoInput{
		SessionID: sid,
		IP:        ip,
		UserAgent: ua,
	})
	if err == nil {
		// Si Identity ya te da access token vigente -> úsalo.
		if info.AccessToken != "" && info.AccessTokenExpiresIn > skewSeconds {
			return info, "", info.AccessToken, nil
		}
		// 2) fallback: si token viene vacío o corto, intenta Redis (NO valida sesión)
		/*
			if tok, expAt, ok := deps.TokenCache.Get(ctx, sid); ok {
				sec := int64(time.Until(expAt).Seconds())
				if sec > skewSeconds {
					info.AccessToken = tok
					info.AccessTokenExpiresIn = sec
					return info, "", tok, nil
				}
			}
		*/

		// Si token falta o está por expirar -> refresh (rota sid)
		ref, rerr := deps.Identity.RefreshSession(ctx, ports.RefreshSessionInput{
			SessionID: sid,
			IP:        ip,
			UserAgent: ua,
		})
		if rerr != nil {
			deps.Cookies.Clear(w)
			w.WriteHeader(http.StatusUnauthorized)
			return ports.GetSessionInfoOutput{}, "", "", rerr
		}
		/*
			if deps.TokenCache != nil && ref.AccessToken != "" && ref.AccessTokenExpiresIn > 0 {
				expAt := time.Now().Add(time.Duration(ref.AccessTokenExpiresIn) * time.Second)
				_ = deps.TokenCache.Set(ctx, ref.SessionID, ref.AccessToken, expAt)
				_ = deps.TokenCache.Del(ctx, sid)
			}
		*/

		// rota cookie
		deps.Cookies.WriteSessionID(w, ref.SessionID, ref.SessionExpiresIn)

		// re-pide info para roles/status/customer_id coherentes con el nuevo sid
		info2, err2 := deps.Identity.GetSessionInfo(ctx, ports.GetSessionInfoInput{
			SessionID: ref.SessionID,
			IP:        ip,
			UserAgent: ua,
		})
		if err2 != nil {
			deps.Cookies.Clear(w)
			w.WriteHeader(http.StatusUnauthorized)
			return ports.GetSessionInfoOutput{}, "", "", err2
		}

		return info2, ref.SessionID, ref.AccessToken, nil
	}

	// 2) clasifica error gRPC
	st, ok := status.FromError(err)
	if !ok {
		http.Error(w, "Session error", http.StatusBadGateway)
		return ports.GetSessionInfoOutput{}, "", "", err
	}

	switch st.Code() {
	case codes.Unauthenticated:
		// solo refresca si es SESSION_EXPIRED (idle TTL venció, pero absolute TTL puede seguir vivo)
		if st.Message() == "SESSION_EXPIRED" {
			ref, rerr := deps.Identity.RefreshSession(ctx, ports.RefreshSessionInput{
				SessionID: sid,
				IP:        ip,
				UserAgent: ua,
			})
			if rerr != nil {
				deps.Cookies.Clear(w)
				w.WriteHeader(http.StatusUnauthorized)
				return ports.GetSessionInfoOutput{}, "", "", rerr
			}

			deps.Cookies.WriteSessionID(w, ref.SessionID, ref.SessionExpiresIn)

			info2, err2 := deps.Identity.GetSessionInfo(ctx, ports.GetSessionInfoInput{
				SessionID: ref.SessionID,
				IP:        ip,
				UserAgent: ua,
			})
			if err2 != nil {
				deps.Cookies.Clear(w)
				w.WriteHeader(http.StatusUnauthorized)
				return ports.GetSessionInfoOutput{}, "", "", err2
			}

			return info2, ref.SessionID, ref.AccessToken, nil
		}

		// revoked / invalid -> limpiar cookie
		deps.Cookies.Clear(w)
		w.WriteHeader(http.StatusUnauthorized)
		return ports.GetSessionInfoOutput{}, "", "", err

	case codes.NotFound, codes.FailedPrecondition:
		// no existe / absolute expired -> no refresh
		deps.Cookies.Clear(w)
		w.WriteHeader(http.StatusUnauthorized)
		return ports.GetSessionInfoOutput{}, "", "", err

	case codes.Unavailable, codes.DeadlineExceeded:
		http.Error(w, "service unavailable", http.StatusServiceUnavailable)
		return ports.GetSessionInfoOutput{}, "", "", err

	default:
		http.Error(w, "Session error", http.StatusBadGateway)
		return ports.GetSessionInfoOutput{}, "", "", err
	}
}
