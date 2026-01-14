package security

import (
	"crypto/rand"
	"encoding/base64"
	"net/http"
	"time"

	"github.com/tagoKoder/bff/internal/config"
)

type CSRFManager struct {
	secure     bool
	sameSite   http.SameSite
	cookieName string
	headerName string
}

func NewCSRFManager(cfg config.Config) *CSRFManager {
	ss := http.SameSiteLaxMode
	switch cfg.CookieSameSite {
	case "strict":
		ss = http.SameSiteStrictMode
	case "none":
		ss = http.SameSiteNoneMode
	}

	name := "__Host-bff_csrf"
	if !cfg.CookieSecure {
		name = "bff_csrf"
	}

	return &CSRFManager{
		secure:     cfg.CookieSecure,
		sameSite:   ss,
		cookieName: name,
		headerName: "X-CSRF-Token",
	}
}

func (m *CSRFManager) EnsureToken(w http.ResponseWriter, r *http.Request) string {
	if ck, err := r.Cookie(m.cookieName); err == nil && ck.Value != "" {
		return ck.Value
	}
	tok := randomToken(32)
	http.SetCookie(w, &http.Cookie{
		Name:     m.cookieName,
		Value:    tok,
		Path:     "/",
		HttpOnly: false, // double-submit requiere que JS lo pueda leer si tu frontend lo maneja
		Secure:   m.secure,
		SameSite: m.sameSite,
		MaxAge:   int((24 * time.Hour).Seconds()),
		Expires:  time.Now().Add(24 * time.Hour),
	})
	return tok
}

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

func randomToken(n int) string {
	b := make([]byte, n)
	_, _ = rand.Read(b)
	return base64.RawURLEncoding.EncodeToString(b)
}
