// bff/internal/api/rest/middleware/csrf.go
package middleware

import (
	"net/http"

	"github.com/tagoKoder/bff/internal/security"
)

func CSRF(m *security.CSRFManager, oas *OpenAPISecurity) func(http.Handler) http.Handler {
	isUnsafe := func(method string) bool {
		switch method {
		case http.MethodPost, http.MethodPut, http.MethodPatch, http.MethodDelete:
			return true
		default:
			return false
		}
	}

	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			ri, ok := oas.Find(r)

			// Si no es una operación OpenAPI conocida o no usa cookieAuth => no CSRF.
			if !ok || !RequiresScheme(ri.Security, "cookieAuth") {
				next.ServeHTTP(w, r)
				return
			}

			// Safe methods: asegura token si hay sesión (mejora UX del frontend).
			if !isUnsafe(r.Method) {
				_ = m.EnsureToken(w, r)
				next.ServeHTTP(w, r)
				return
			}

			// Mutables: sólo valida si OpenAPI pide csrfAuth además de cookieAuth (AND).
			if RequiresBothInSameRequirement(ri.Security, "cookieAuth", "csrfAuth") {
				if !m.Validate(r) {
					w.WriteHeader(http.StatusForbidden)
					return
				}
			}

			next.ServeHTTP(w, r)
		})
	}
}
