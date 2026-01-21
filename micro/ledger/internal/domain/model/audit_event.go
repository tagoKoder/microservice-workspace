// micro\ledger\internal\domain\model\audit_event.go
package model

type AuditEventV1 struct {
	Version       string `json:"version"`
	EventID       string `json:"event_id"`
	OccurredAt    string `json:"occurred_at"`
	Service       string `json:"service"`
	Environment   string `json:"environment"`
	CorrelationID string `json:"correlation_id"`

	RouteTemplate string `json:"route_template"`
	Action        string `json:"action"`

	Actor struct {
		Sub        string   `json:"sub"`
		CustomerID string   `json:"customer_id,omitempty"`
		Roles      []string `json:"roles,omitempty"`
		MFA        bool     `json:"mfa,omitempty"`
	} `json:"actor"`

	Authorization struct {
		Engine   string `json:"engine"`
		Decision string `json:"decision,omitempty"` // solo si AVP
		PolicyID string `json:"policy_id,omitempty"`
	} `json:"authorization"`

	RequestContext struct {
		Channel        string `json:"channel,omitempty"`
		IPHash         string `json:"ip_hash,omitempty"`
		UAHash         string `json:"ua_hash,omitempty"`
		IdempotencyKey string `json:"idempotency_key,omitempty"`
	} `json:"request_context"`

	Entity struct {
		Type string `json:"type"`
		ID   string `json:"id"`
	} `json:"entity"`

	Details map[string]any `json:"details,omitempty"`
}
