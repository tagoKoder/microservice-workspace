package middleware

import (
	"net/http"
	"strings"

	"github.com/tagoKoder/bff/internal/security"
)

type CSRFRules struct {
	// Prefijos públicos (no CSRF)
	PublicPrefixes []string
	// Prefijos protegidos por cookieAuth (sí CSRF en métodos mutables)
	ProtectedPrefixes []string
}

func CSRF(m *security.CSRFManager, rules CSRFRules) func(http.Handler) http.Handler {
	isPublic := func(p string) bool {
		for _, pref := range rules.PublicPrefixes {
			if strings.HasPrefix(p, pref) {
				return true
			}
		}
		return false
	}
	isProtected := func(p string) bool {
		for _, pref := range rules.ProtectedPrefixes {
			if strings.HasPrefix(p, pref) {
				return true
			}
		}
		return false
	}

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
			if isPublic(r.URL.Path) {
				next.ServeHTTP(w, r)
				return
			}

			// Para GET/HEAD en rutas protegidas, emitimos token si no existe.
			if isProtected(r.URL.Path) && !isUnsafe(r.Method) {
				_ = m.EnsureToken(w, r)
				next.ServeHTTP(w, r)
				return
			}

			// Para métodos mutables, exigir CSRF en rutas protegidas.
			if isProtected(r.URL.Path) && isUnsafe(r.Method) {
				if !m.Validate(r) {
					w.WriteHeader(http.StatusForbidden)
					return
				}
			}

			next.ServeHTTP(w, r)
		})
	}
}
