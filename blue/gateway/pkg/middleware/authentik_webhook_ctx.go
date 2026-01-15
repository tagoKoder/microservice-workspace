package middleware

import (
	"context"
	"crypto/subtle"
)

var authentikWebhookKey struct{}

// WithAuthentikWebhook marca el contexto como proveniente del webhook de Authentik.
func WithAuthentikWebhook(ctx context.Context) context.Context {
	return context.WithValue(ctx, authentikWebhookKey, true)
}

// IsAuthentikWebhook indica si la request proviene del webhook de Authentik.
func IsAuthentikWebhook(ctx context.Context) bool {
	v, ok := ctx.Value(authentikWebhookKey).(bool)
	return ok && v
}

// ConstantTimeEqual compara strings en tiempo constante.
func ConstantTimeEqual(a, b string) bool {
	return subtle.ConstantTimeCompare([]byte(a), []byte(b)) == 1
}
