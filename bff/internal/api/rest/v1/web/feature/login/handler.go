package login

import (
	"net/http"

	"github.com/tagoKoder/bff/internal/client/ports"
	"github.com/tagoKoder/bff/internal/config"
	"github.com/tagoKoder/bff/internal/security"
)

type Handler struct {
	cfg      config.Config
	clients  *ports.Clients
	cookies  *security.CookieManager
	csrf     *security.CSRFManager
	redirect *security.RedirectPolicy
}

func New(cfg config.Config, clients *ports.Clients, cookies *security.CookieManager, csrf *security.CSRFManager, redirect *security.RedirectPolicy) *Handler {
	return &Handler{cfg: cfg, clients: clients, cookies: cookies, csrf: csrf, redirect: redirect}
}

// GET /bff/login/start?redirect=/home
func (h *Handler) Start(w http.ResponseWriter, r *http.Request) {
	redir := r.URL.Query().Get("redirect")
	redir = h.redirect.Safe(redir)

	out, err := h.clients.Identity.StartOidcLogin(r.Context(), ports.StartOidcLoginInput{
		Channel:            "web",
		RedirectAfterLogin: redir,
	})
	if err != nil {
		w.WriteHeader(http.StatusBadGateway)
		return
	}

	// En banking web típicamente rediriges al IdP
	http.Redirect(w, r, out.AuthorizationURL, http.StatusFound)
}

// GET /bff/login/callback?code=...&state=...
func (h *Handler) Callback(w http.ResponseWriter, r *http.Request) {
	code := r.URL.Query().Get("code")
	state := r.URL.Query().Get("state")
	if code == "" || state == "" {
		w.WriteHeader(http.StatusBadRequest)
		return
	}

	out, err := h.clients.Identity.CompleteOidcLogin(r.Context(), ports.CompleteOidcLoginInput{
		Code:      code,
		State:     state,
		IP:        r.RemoteAddr,
		UserAgent: r.UserAgent(),
		Channel:   "web",
	})
	if err != nil {
		w.WriteHeader(http.StatusUnauthorized)
		return
	}

	// Cookie de sesión + CSRF token
	h.cookies.Set(w, out.SessionID, out.SessionExpiresIn)
	_ = h.csrf.EnsureToken(w, r)

	// Evita cache
	w.Header().Set("Cache-Control", "no-store")

	http.Redirect(w, r, h.redirect.Safe(out.RedirectAfterLogin), http.StatusFound)
}
