package middleware

import (
	"context"
	"net/http"
	"strings"

	"github.com/coreos/go-oidc/v3/oidc"
)

type OIDC struct {
	p        *oidc.Provider
	verifier *oidc.IDTokenVerifier
	issuer   string
}

// NewOIDC carga el .well-known y prepara el verificador (SkipClientIDCheck
// porque en Authentik el access_token suele ser JWT con 'aud' != client_id).
func NewOIDC(ctx context.Context, issuer string) (*OIDC, error) {
	p, err := oidc.NewProvider(ctx, issuer)
	if err != nil {
		return nil, err
	}
	v := p.Verifier(&oidc.Config{SkipClientIDCheck: true})
	return &OIDC{p: p, verifier: v, issuer: issuer}, nil
}

type ctxKey string

var AccessTokenKey ctxKey = "accessToken"

// Auth valida el token (firma/exp/iss) y lo inyecta al contexto.
func (o *OIDC) Auth(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		h := r.Header.Get("Authorization")
		if h == "" {
			http.Error(w, "missing Authorization", http.StatusUnauthorized)
			return
		}
		raw := strings.TrimSpace(strings.TrimPrefix(h, "Bearer"))
		if raw == "" {
			http.Error(w, "invalid Authorization", http.StatusUnauthorized)
			return
		}
		// Verifica como IDToken (vale para JWT access tokens comunes).
		if _, err := o.verifier.Verify(r.Context(), raw); err != nil {
			http.Error(w, "invalid token", http.StatusUnauthorized)
			return
		}
		ctx := context.WithValue(r.Context(), AccessTokenKey, raw)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}
