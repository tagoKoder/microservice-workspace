package security

import (
	"net/http"
	"time"

	"github.com/tagoKoder/bff/internal/config"
)

type CookieManager struct {
	secure     bool
	sameSite   http.SameSite
	cookieName string
}

func NewCookieManager(cfg config.Config) *CookieManager {
	ss := http.SameSiteLaxMode
	switch cfg.CookieSameSite {
	case "strict":
		ss = http.SameSiteStrictMode
	case "none":
		ss = http.SameSiteNoneMode
	}

	// __Host- requiere Secure; si estás en local sin HTTPS, usa fallback.
	name := "__Host-bff_sid"
	if !cfg.CookieSecure {
		name = "bff_sid"
	}

	return &CookieManager{
		secure:     cfg.CookieSecure,
		sameSite:   ss,
		cookieName: name,
	}
}

func (c *CookieManager) Name() string { return c.cookieName }

func (m *CookieManager) Set(w http.ResponseWriter, sessionID string, expiresInSeconds int64) {
	if sessionID == "" {
		return
	}

	exp := time.Now().Add(time.Duration(expiresInSeconds) * time.Second)
	http.SetCookie(w, &http.Cookie{
		Name:     m.cookieName, // asumiendo que tu struct tiene cookieName
		Value:    sessionID,
		Path:     "/",
		HttpOnly: true,
		Secure:   m.secure,   // asumiendo que existe
		SameSite: m.sameSite, // asumiendo que existe
		Expires:  exp,
		MaxAge:   int(expiresInSeconds),
	})
}

func (c *CookieManager) ReadSessionID(r *http.Request) (string, bool) {
	// Lee primero el nombre actual
	if ck, err := r.Cookie(c.cookieName); err == nil && ck.Value != "" {
		return ck.Value, true
	}
	// Fallback: si estás en prod con __Host- pero algún entorno dejó bff_sid, o viceversa
	alts := []string{"__Host-bff_sid", "bff_sid"}
	for _, n := range alts {
		if n == c.cookieName {
			continue
		}
		if ck, err := r.Cookie(n); err == nil && ck.Value != "" {
			return ck.Value, true
		}
	}
	return "", false
}

func (c *CookieManager) Clear(w http.ResponseWriter) {
	cookie := &http.Cookie{
		Name:     c.cookieName,
		Value:    "",
		Path:     "/",
		HttpOnly: true,
		Secure:   c.secure,
		SameSite: c.sameSite,
		MaxAge:   -1,
		Expires:  time.Unix(0, 0),
	}
	http.SetCookie(w, cookie)

	// Limpia también el alternate por higiene
	for _, n := range []string{"__Host-bff_sid", "bff_sid"} {
		if n == c.cookieName {
			continue
		}
		http.SetCookie(w, &http.Cookie{
			Name:     n,
			Value:    "",
			Path:     "/",
			HttpOnly: true,
			Secure:   c.secure,
			SameSite: c.sameSite,
			MaxAge:   -1,
			Expires:  time.Unix(0, 0),
		})
	}
}
