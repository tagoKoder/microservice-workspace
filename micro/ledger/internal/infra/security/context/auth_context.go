// micro\ledger\internal\infra\security\context\auth_context.go
package authctx

import (
	"context"
)

type ctxKey string

const (
	keyCorrelationID ctxKey = "correlation_id"
	keyAccessToken   ctxKey = "access_token"

	keyRouteTemplate ctxKey = "route_template"
	keyActionID      ctxKey = "action_id"

	keyAuthzDecision ctxKey = "authz_decision" // "ALLOW"|"DENY"
	keyAuthzPolicyID ctxKey = "authz_policy_id"

	keyIPHash      ctxKey = "ip_hash"
	keyUAHash      ctxKey = "ua_hash"
	keyIdempotency ctxKey = "idempotency_key"

	keyActor ctxKey = "actor"
)

type Actor struct {
	Subject    string   `json:"sub"`
	CustomerID string   `json:"customer_id,omitempty"`
	Roles      []string `json:"roles,omitempty"`
	MFA        bool     `json:"mfa,omitempty"`
}

func WithCorrelationID(ctx context.Context, cid string) context.Context {
	return context.WithValue(ctx, keyCorrelationID, cid)
}
func CorrelationID(ctx context.Context) string {
	v, _ := ctx.Value(keyCorrelationID).(string)
	return v
}

func WithAccessToken(ctx context.Context, tok string) context.Context {
	return context.WithValue(ctx, keyAccessToken, tok)
}
func AccessToken(ctx context.Context) string {
	v, _ := ctx.Value(keyAccessToken).(string)
	return v
}

func WithRouteTemplate(ctx context.Context, rt string) context.Context {
	return context.WithValue(ctx, keyRouteTemplate, rt)
}
func RouteTemplate(ctx context.Context) string {
	v, _ := ctx.Value(keyRouteTemplate).(string)
	return v
}

func WithActionID(ctx context.Context, a string) context.Context {
	return context.WithValue(ctx, keyActionID, a)
}
func ActionID(ctx context.Context) string {
	v, _ := ctx.Value(keyActionID).(string)
	return v
}

func WithAuthzDecision(ctx context.Context, d string) context.Context {
	return context.WithValue(ctx, keyAuthzDecision, d)
}
func AuthzDecision(ctx context.Context) string {
	v, _ := ctx.Value(keyAuthzDecision).(string)
	return v
}

func WithAuthzPolicyID(ctx context.Context, pid string) context.Context {
	return context.WithValue(ctx, keyAuthzPolicyID, pid)
}
func AuthzPolicyID(ctx context.Context) string {
	v, _ := ctx.Value(keyAuthzPolicyID).(string)
	return v
}

func WithIPHash(ctx context.Context, h string) context.Context {
	return context.WithValue(ctx, keyIPHash, h)
}
func IPHash(ctx context.Context) string {
	v, _ := ctx.Value(keyIPHash).(string)
	return v
}

func WithUAHash(ctx context.Context, h string) context.Context {
	return context.WithValue(ctx, keyUAHash, h)
}
func UAHash(ctx context.Context) string {
	v, _ := ctx.Value(keyUAHash).(string)
	return v
}

func WithIdempotencyKey(ctx context.Context, k string) context.Context {
	return context.WithValue(ctx, keyIdempotency, k)
}
func IdempotencyKey(ctx context.Context) string {
	v, _ := ctx.Value(keyIdempotency).(string)
	return v
}

func WithActor(ctx context.Context, a Actor) context.Context {
	return context.WithValue(ctx, keyActor, a)
}
func ActorFrom(ctx context.Context) Actor {
	v, _ := ctx.Value(keyActor).(Actor)
	return v
}
