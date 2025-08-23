package middleware

import (
	"context"
	"net/http"

	pkgMiddleware "github.com/tagoKoder/gateway/pkg/middleware"
)

type AccessTokenVerifier interface {
	Verify(next http.Handler) http.Handler
}

// Implementación OIDC que delega la verificación al pkg/oidc.
type OIDCAccessTokenVerifier struct {
	VerifyFunc func(ctx context.Context, raw string) error
}

func NewOIDCAccessTokenVerifier(verify func(context.Context, string) error) *OIDCAccessTokenVerifier {
	return &OIDCAccessTokenVerifier{VerifyFunc: verify}
}

func (m *OIDCAccessTokenVerifier) Verify(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		raw, err := pkgMiddleware.ExtractBearer(r.Header.Get("Authorization"))
		if err != nil {
			http.Error(w, "missing or invalid Authorization header", http.StatusUnauthorized)
			return
		}
		if err := m.VerifyFunc(r.Context(), raw); err != nil {
			http.Error(w, "invalid token", http.StatusUnauthorized)
			return
		}
		next.ServeHTTP(w, r.WithContext(pkgMiddleware.WithAccessToken(r.Context(), raw)))
	})
}
