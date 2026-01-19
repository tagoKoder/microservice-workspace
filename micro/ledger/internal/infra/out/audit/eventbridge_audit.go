package audit

import (
	"context"
	"encoding/json"
	"log"
	"time"

	"github.com/aws/aws-sdk-go-v2/service/eventbridge"
	ebtypes "github.com/aws/aws-sdk-go-v2/service/eventbridge/types"
	out "github.com/tagoKoder/ledger/internal/domain/port/out"
	authctx "github.com/tagoKoder/ledger/internal/infra/security/context"
	"github.com/tagoKoder/ledger/internal/domain/model"
)

type EventBridgeAudit struct {
	eb         *eventbridge.Client
	busName    string
	source     string
	detailType string
	service    string
	env        string
}

func NewEventBridgeAudit(eb *eventbridge.Client, busName, source, detailType, service, env string) out.AuditPort {
	return &EventBridgeAudit{
		eb: eb, busName: busName, source: source, detailType: detailType,
		service: service, env: env,
	}
}



func (a *EventBridgeAudit) Record(ctx context.Context, action, entity, entityID, actor string, at time.Time, details map[string]any) error {
	ev := model.AuditEventV1{
		Version:      "1.0",
		EventID:      newEventID(),
		OccurredAt:   at.UTC().Format(time.RFC3339Nano),
		Service:      a.service,
		Environment:  a.env,
		CorrelationID: authctx.CorrelationID(ctx),
		RouteTemplate: authctx.RouteTemplate(ctx),
		Action:        authctx.ActionID(ctx),
		Entity:        struct {
			Type string `json:"type"`
			ID   string `json:"id"`
		}{Type: entity, ID: entityID},
		Details:       details,
	}

	act := authctx.ActorFrom(ctx)
	ev.Actor.Sub = act.Subject
	ev.Actor.CustomerID = act.CustomerID
	ev.Actor.Roles = act.Roles
	ev.Actor.MFA = act.MFA

	ev.Authorization.Engine = "avp"
	// decision solo si AVP (si no lo tienes en ctx, queda vacío)
	ev.Authorization.Decision = authctx.AuthzDecision(ctx)
	ev.Authorization.PolicyID = authctx.AuthzPolicyID(ctx)

	ev.RequestContext.Channel = "web"
	ev.RequestContext.IPHash = authctx.IPHash(ctx)
	ev.RequestContext.UAHash = authctx.UAHash(ctx)
	ev.RequestContext.IdempotencyKey = authctx.IdempotencyKey(ctx)

	// Permite sobreescribir action catálogo si te pasan "action" externo
	// (pero tu estándar prioriza catálogo derivado de resolver)
	_ = action
	_ = actor

	b, err := json.Marshal(ev)
	if err != nil {
		// best-effort
		log.Printf("audit marshal: %v", err)
		return nil
	}

	entry := ebtypes.PutEventsRequestEntry{
		Source:     &a.source,
		DetailType: &a.detailType,
		Detail:     ptr(string(b)),
		Time:       ptrTime(at.UTC()),
	}
	if a.busName != "" {
		entry.EventBusName = &a.busName
	}

	resp, err := a.eb.PutEvents(ctx, &eventbridge.PutEventsInput{
		Entries: []ebtypes.PutEventsRequestEntry{entry},
	})
	if err != nil {
		log.Printf("audit put-events error: %v", err) // fallback log (sin PII)
		return nil
	}
	if resp.FailedEntryCount > 0 {
		log.Printf("audit put-events failed entries: %d", resp.FailedEntryCount)
	}
	return nil
}

func newEventID() string { return time.Now().UTC().Format("20060102T150405.000000000Z07:00") }

func ptr(s string) *string { return &s }
func ptrTime(t time.Time) *time.Time { return &t }
