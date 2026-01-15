// gateway/internal/entrypoint/http/middleware
package middleware

import (
	"context"
	"net/http"

	pkgMiddleware "github.com/tagoKoder/gateway/pkg/middleware"
	pkgOidc "github.com/tagoKoder/gateway/pkg/oidc"
)

type AccessTokenVerifier interface {
	Verify(next http.Handler) http.Handler
}

// Implementación OIDC que delega la verificación al pkg/oidc.
type OIDCAccessTokenVerifier struct {
	VerifyFunc func(ctx context.Context, raw string) error
}

func NewOIDCAccessTokenVerifier(v *pkgOidc.Verifier) *OIDCAccessTokenVerifier {
	return &OIDCAccessTokenVerifier{
		VerifyFunc: func(ctx context.Context, raw string) error {
			_, err := v.VerifyAccessToken(ctx, raw)
			return err
		},
	}
}
func (m *OIDCAccessTokenVerifier) Verify(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// Deja pasar preflight CORS
		if r.Method == http.MethodOptions {
			w.WriteHeader(http.StatusNoContent)
			return
		}

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
