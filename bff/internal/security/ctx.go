// bff\internal\security\ctx.go
package security

import "context"

type ctxKey string

const (
	CtxCorrelationID ctxKey = "correlation_id"
	CtxAccessToken   ctxKey = "access_token"
	CtxSessionID     ctxKey = "session_id"

	// NEW
	CtxRouteTemplate  ctxKey = "route_template"
	CtxClientIP       ctxKey = "client_ip"
	CtxUserAgent      ctxKey = "user_agent"
	CtxIdempotencyKey ctxKey = "idempotency_key"
)

func WithCorrelationID(ctx context.Context, v string) context.Context {
	if v == "" {
		return ctx
	}
	return context.WithValue(ctx, CtxCorrelationID, v)
}
func CorrelationID(ctx context.Context) string {
	v, _ := ctx.Value(CtxCorrelationID).(string)
	return v
}

func WithAccessToken(ctx context.Context, v string) context.Context {
	if v == "" {
		return ctx
	}
	return context.WithValue(ctx, CtxAccessToken, v)
}
func AccessToken(ctx context.Context) string {
	v, _ := ctx.Value(CtxAccessToken).(string)
	return v
}

func WithSessionID(ctx context.Context, v string) context.Context {
	if v == "" {
		return ctx
	}
	return context.WithValue(ctx, CtxSessionID, v)
}
func SessionID(ctx context.Context) string {
	v, _ := ctx.Value(CtxSessionID).(string)
	return v
}

// NEW helpers
func WithRouteTemplate(ctx context.Context, v string) context.Context {
	if v == "" {
		return ctx
	}
	return context.WithValue(ctx, CtxRouteTemplate, v)
}
func RouteTemplate(ctx context.Context) string {
	v, _ := ctx.Value(CtxRouteTemplate).(string)
	return v
}

func WithClientIP(ctx context.Context, v string) context.Context {
	if v == "" {
		return ctx
	}
	return context.WithValue(ctx, CtxClientIP, v)
}
func ClientIP(ctx context.Context) string {
	v, _ := ctx.Value(CtxClientIP).(string)
	return v
}

func WithUserAgent(ctx context.Context, v string) context.Context {
	if v == "" {
		return ctx
	}
	return context.WithValue(ctx, CtxUserAgent, v)
}
func UserAgent(ctx context.Context) string {
	v, _ := ctx.Value(CtxUserAgent).(string)
	return v
}

func WithIdempotencyKey(ctx context.Context, v string) context.Context {
	if v == "" {
		return ctx
	}
	return context.WithValue(ctx, CtxIdempotencyKey, v)
}
func IdempotencyKey(ctx context.Context) string {
	v, _ := ctx.Value(CtxIdempotencyKey).(string)
	return v
}
