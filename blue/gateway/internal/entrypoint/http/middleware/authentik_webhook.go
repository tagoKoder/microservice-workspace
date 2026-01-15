package middleware

import (
	"net/http"
	"strings"

	pkgmw "github.com/tagoKoder/gateway/pkg/middleware"
)

type AuthentikWebhookOptions struct {
	// Lista de secretos válidos (permite rotación). Al menos 1 requerido.
	Secrets []string

	// Nombre del header; por defecto "Authorization".
	HeaderName string

	// Prefijo del header; por defecto "Bearer ".
	Prefix string
}

// AuthentikWebhookVerifier crea el middleware para validar el webhook de Authentik.
func AuthentikWebhookVerifier(opts *AuthentikWebhookOptions) func(http.Handler) http.Handler {
	header := opts.HeaderName
	if header == "" {
		header = "Authorization"
	}
	prefix := opts.Prefix
	if prefix == "" {
		prefix = "Bearer "
	}

	// Copia local para evitar modificaciones externas.
	secrets := append([]string(nil), opts.Secrets...)

	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			raw := r.Header.Get(header)
			if !strings.HasPrefix(raw, prefix) {
				http.Error(w, "unauthorized authentik webhook", http.StatusUnauthorized)
				return
			}
			token := raw[len(prefix):]

			ok := false
			for _, s := range secrets {
				if s == "" {
					continue
				}
				if pkgmw.ConstantTimeEqual(token, s) {
					ok = true
					break
				}
			}
			if !ok {
				http.Error(w, "unauthorized authentik webhook", http.StatusUnauthorized)
				return
			}

			// Marca el contexto: útil para logs/metricas/handlers
			ctx := pkgmw.WithAuthentikWebhook(r.Context())
			next.ServeHTTP(w, r.WithContext(ctx))
		})
	}
}
