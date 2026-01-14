package audit

import (
	"context"
	"encoding/json"
	"time"

	out "github.com/tagoKoder/ledger/internal/domain/port/out"
)

type KafkaAudit struct {
	pub   out.EventPublisherPort
	topic string
}

func NewKafkaAudit(pub out.EventPublisherPort, topic string) out.AuditPort {
	return &KafkaAudit{pub: pub, topic: topic}
}

type auditEvent struct {
	Action   string         `json:"action"`
	Entity   string         `json:"entity"`
	EntityID string         `json:"entity_id"`
	Actor    string         `json:"actor"`
	At       string         `json:"at"`
	Details  map[string]any `json:"details,omitempty"`
}

func (a *KafkaAudit) Record(ctx context.Context, action, entity, entityID, actor string, at time.Time, details map[string]any) error {
	ev := auditEvent{
		Action: action, Entity: entity, EntityID: entityID,
		Actor: actor, At: at.UTC().Format(time.RFC3339Nano),
		Details: details,
	}
	b, _ := json.Marshal(ev)
	return a.pub.Publish(ctx, a.topic, entityID, b)
}
