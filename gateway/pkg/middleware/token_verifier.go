package middleware

import (
	"context"
	"errors"
	"strings"
)

type ctxKey string

const AccessTokenKey ctxKey = "access_token"

func WithAccessToken(ctx context.Context, token string) context.Context {
	return context.WithValue(ctx, AccessTokenKey, token)
}

func AccessTokenFrom(ctx context.Context) (string, bool) {
	t, ok := ctx.Value(AccessTokenKey).(string)
	return t, ok
}

// ---- extracción robusta del Bearer ----
func ExtractBearer(h string) (string, error) {
	if h == "" {
		return "", errors.New("missing")
	}
	s := strings.TrimSpace(h)
	if len(s) < 7 || !strings.EqualFold(s[:6], "bearer") {
		return "", errors.New("invalid")
	}
	s = strings.TrimSpace(s[6:])
	if s == "" {
		return "", errors.New("invalid")
	}
	return s, nil
}
