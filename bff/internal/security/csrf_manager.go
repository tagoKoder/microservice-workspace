// bff\internal\security\csrf_manager.go
package security

import (
	"crypto/rand"
	"encoding/base64"
	"fmt"
	"net/http"
	"time"

	"github.com/tagoKoder/bff/internal/config"
)

type CSRFManager struct {
	secure     bool
	sameSite   http.SameSite
	cookieName string
	headerName string

	ttl time.Duration
}

func NewCSRFManager(cfg config.Config) *CSRFManager {
	// SameSite: si pones "none" pero no es Secure, navegadores lo bloquean.
	ss := http.SameSiteLaxMode
	switch cfg.CookieSameSite {
	case "strict":
		ss = http.SameSiteStrictMode
	case "none":
		if cfg.CookieSecure {
			ss = http.SameSiteNoneMode
		} else {
			// downgrade seguro
			ss = http.SameSiteLaxMode
		}
	}

	// __Host- requiere Secure=true, Path=/ y sin Domain
	name := "__Host-bff_csrf"
	if !cfg.CookieSecure {
		name = "bff_csrf"
	}

	return &CSRFManager{
		secure:     cfg.CookieSecure,
		sameSite:   ss,
		cookieName: name,
		headerName: "X-CSRF-Token",
		ttl:        24 * time.Hour,
	}
}

// EnsureToken garantiza que exista cookie CSRF.
// No devuelve el token (para no acoplar el middleware); sólo lo setea si falta.
func (m *CSRFManager) EnsureToken(w http.ResponseWriter, r *http.Request) error {
	if ck, err := r.Cookie(m.cookieName); err == nil && ck.Value != "" {
		return nil
	}

	tok, err := randomToken(32)
	if err != nil {
		return err
	}

	http.SetCookie(w, &http.Cookie{
		Name:     m.cookieName,
		Value:    tok,
		Path:     "/",
		HttpOnly: false, // double-submit: frontend puede leerlo si lo necesita
		Secure:   m.secure,
		SameSite: m.sameSite,
		MaxAge:   int(m.ttl.Seconds()),
		Expires:  time.Now().Add(m.ttl),
	})

	return nil
}

// Validate aplica double-submit:
// header X-CSRF-Token debe ser igual al valor de la cookie CSRF.
func (m *CSRFManager) Validate(r *http.Request) bool {
	ck, err := r.Cookie(m.cookieName)
	if err != nil || ck.Value == "" {
		return false
	}

	h := r.Header.Get(m.headerName)
	return h != "" && h == ck.Value
}

func (m *CSRFManager) Clear(w http.ResponseWriter) {
	http.SetCookie(w, &http.Cookie{
		Name:     m.cookieName,
		Value:    "",
		Path:     "/",
		Secure:   m.secure,
		SameSite: m.sameSite,
		MaxAge:   -1,
		Expires:  time.Unix(0, 0),
	})
}

// Útil si tienes un endpoint tipo GET /api/v1/session/csrf que retorna el token.
func (m *CSRFManager) ReadToken(r *http.Request) (string, bool) {
	ck, err := r.Cookie(m.cookieName)
	if err != nil || ck.Value == "" {
		return "", false
	}
	return ck.Value, true
}

func randomToken(n int) (string, error) {
	b := make([]byte, n)
	if _, err := rand.Read(b); err != nil {
		return "", fmt.Errorf("csrf rand: %w", err)
	}
	return base64.RawURLEncoding.EncodeToString(b), nil
}
