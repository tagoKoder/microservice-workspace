// bff\internal\security\ctx.go
package security

import "context"

type ctxKey string

const (
	CtxCorrelationID ctxKey = "correlation_id"
	CtxAccessToken   ctxKey = "access_token"
	CtxSessionID     ctxKey = "session_id"
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
