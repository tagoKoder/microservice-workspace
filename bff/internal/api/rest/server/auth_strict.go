package server

import (
	"context"
	"net"
	"net/http"

	openapi "github.com/tagoKoder/bff/internal/api/rest/gen/openapi"
	"github.com/tagoKoder/bff/internal/api/rest/middleware"
	"github.com/tagoKoder/bff/internal/client/ports"
)

// GET /bff/login/start?redirect=/home
func (s *Server) StartWebLogin(
	ctx context.Context,
	req openapi.StartWebLoginRequestObject,
) (openapi.StartWebLoginResponseObject, error) {

	redir := ""
	if req.Params.Redirect != nil {
		redir = *req.Params.Redirect
	}
	redir = s.deps.Redirect.Safe(redir)

	out, err := s.deps.Clients.Identity.StartOidcLogin(ctx, ports.StartOidcLoginInput{
		Channel:            "web",
		RedirectAfterLogin: redir,
	})
	if err != nil {
		return openapi.StartWebLogin502JSONResponse(openapi.ErrorResponse{
			Code:    "UPSTREAM_ERROR",
			Message: "identity service unavailable",
		}), nil
	}

	// 302 Location: out.AuthorizationURL
	return openapi.StartWebLogin302Response{
		Headers: openapi.StartWebLogin302ResponseHeaders{
			Location: out.AuthorizationURL,
		},
	}, nil
}

// GET /bff/login/callback?code=...&state=...
func (s *Server) CompleteWebLogin(
	ctx context.Context,
	req openapi.CompleteWebLoginRequestObject,
) (openapi.CompleteWebLoginResponseObject, error) {

	// Tomamos request/response del contexto (inyectado por strict middleware)
	r, ok := middleware.GetHTTPRequest(ctx)
	if !ok || r == nil {
		return openapi.CompleteWebLogin502JSONResponse(openapi.ErrorResponse{
			Code:    "INTERNAL",
			Message: "missing http request context",
		}), nil
	}
	w, ok := middleware.GetHTTPResponseWriter(ctx)
	if !ok || w == nil {
		return openapi.CompleteWebLogin502JSONResponse(openapi.ErrorResponse{
			Code:    "INTERNAL",
			Message: "missing http response context",
		}), nil
	}

	code := req.Params.Code
	state := req.Params.State
	if code == "" || state == "" {
		return openapi.CompleteWebLogin400JSONResponse(openapi.ErrorResponse{
			Code:    "BAD_REQUEST",
			Message: "missing code/state",
		}), nil
	}

	out, err := s.deps.Clients.Identity.CompleteOidcLogin(ctx, ports.CompleteOidcLoginInput{
		Code:      code,
		State:     state,
		IP:        r.RemoteAddr,
		UserAgent: r.UserAgent(),
		Channel:   "web",
	})
	if err != nil {
		return openapi.CompleteWebLogin401JSONResponse(openapi.ErrorResponse{
			Code:    "UNAUTHORIZED",
			Message: "login rejected",
		}), nil
	}

	// Cookie de sesión + CSRF token
	s.deps.Cookies.Set(w, out.SessionID, out.SessionExpiresIn)
	_ = s.deps.CSRF.EnsureToken(w, r)

	// Evita cache
	w.Header().Set("Cache-Control", "no-store")

	return openapi.CompleteWebLogin302Response{
		Headers: openapi.CompleteWebLogin302ResponseHeaders{
			Location: s.deps.Redirect.Safe(out.RedirectAfterLogin),
		},
	}, nil
}

// POST /bff/session/logout
func (s *Server) LogoutWebSession(
	ctx context.Context,
	_ openapi.LogoutWebSessionRequestObject,
) (openapi.LogoutWebSessionResponseObject, error) {

	r, ok := middleware.GetHTTPRequest(ctx)
	if !ok || r == nil {
		return openapi.LogoutWebSession502JSONResponse(openapi.ErrorResponse{
			Code:    "INTERNAL",
			Message: "missing http request context",
		}), nil
	}
	w, ok := middleware.GetHTTPResponseWriter(ctx)
	if !ok || w == nil {
		return openapi.LogoutWebSession502JSONResponse(openapi.ErrorResponse{
			Code:    "INTERNAL",
			Message: "missing http response context",
		}), nil
	}

	sid, ok := s.deps.Cookies.ReadSessionID(r)
	if ok && sid != "" {
		_, _ = s.deps.Clients.Identity.LogoutSession(ctx, ports.LogoutSessionInput{SessionID: sid})
	}

	s.deps.Cookies.Clear(w)
	s.deps.CSRF.Clear(w)

	return openapi.LogoutWebSession204Response{}, nil
}

// GET /api/v1/session/csrf
func (s *Server) GetWebCsrfToken(
	ctx context.Context,
	_ openapi.GetWebCsrfTokenRequestObject,
) (openapi.GetWebCsrfTokenResponseObject, error) {

	r, ok := middleware.GetHTTPRequest(ctx)
	if !ok || r == nil {
		return openapi.GetWebCsrfToken403JSONResponse(openapi.ErrorResponse{
			Code:    "INTERNAL",
			Message: "missing http request context",
		}), nil
	}
	w, ok := middleware.GetHTTPResponseWriter(ctx)
	if !ok || w == nil {
		return openapi.GetWebCsrfToken403JSONResponse(openapi.ErrorResponse{
			Code:    "INTERNAL",
			Message: "missing http response context",
		}), nil
	}

	// 1) asegurar cookie
	if err := s.deps.CSRF.EnsureToken(w, r); err != nil {
		return openapi.GetWebCsrfToken502JSONResponse(openapi.ErrorResponse{
			Code:    "INTERNAL",
			Message: "cannot issue csrf token",
		}), nil
	}

	// 2) leerla
	tok, ok := s.deps.CSRF.ReadToken(r)
	if !ok || tok == "" {
		return openapi.GetWebCsrfToken502JSONResponse(openapi.ErrorResponse{
			Code:    "INTERNAL",
			Message: "csrf token missing after issuance",
		}), nil
	}

	return openapi.GetWebCsrfToken200JSONResponse(openapi.CsrfTokenResponse{
		CsrfToken: tok,
	}), nil
}

// ===============================
// === STRICT HANDLERS FOR WEBAUTHN ======
// ===============================

func ipOnly(r *http.Request) string {
	host, _, err := net.SplitHostPort(r.RemoteAddr)
	if err != nil {
		return r.RemoteAddr
	}
	return host
}

// POST /bff/session/refresh
func (s *Server) RefreshWebSession(
	ctx context.Context,
	req openapi.RefreshWebSessionRequestObject,
) (openapi.RefreshWebSessionResponseObject, error) {
	_ = req.Params.XCSRFToken // validado por middleware CSRF/OpenAPI

	r, ok := middleware.GetHTTPRequest(ctx)
	if !ok || r == nil {
		return openapi.RefreshWebSession502JSONResponse(openapi.ErrorResponse{
			Code:    "INTERNAL",
			Message: "missing http request context",
		}), nil
	}
	w, ok := middleware.GetHTTPResponseWriter(ctx)
	if !ok || w == nil {
		return openapi.RefreshWebSession502JSONResponse(openapi.ErrorResponse{
			Code:    "INTERNAL",
			Message: "missing http response context",
		}), nil
	}

	sid, ok := s.deps.Cookies.ReadSessionID(r)
	if !ok || sid == "" {
		return openapi.RefreshWebSession401JSONResponse(openapi.ErrorResponse{
			Code:    "UNAUTHORIZED",
			Message: "missing session",
		}), nil
	}

	out, err := s.deps.Clients.Identity.RefreshSession(ctx, ports.RefreshSessionInput{
		SessionID: sid,
		IP:        ipOnly(r),
		UserAgent: r.UserAgent(),
	})
	if err != nil {
		// higiene: si refresh falla, limpia para forzar login
		s.deps.Cookies.Clear(w)
		s.deps.CSRF.Clear(w)

		return openapi.RefreshWebSession401JSONResponse(openapi.ErrorResponse{
			Code:    "UNAUTHORIZED",
			Message: "session refresh rejected",
		}), nil
	}

	// rota cookie
	s.deps.Cookies.Set(w, out.SessionID, out.SessionExpiresIn)
	_ = s.deps.CSRF.EnsureToken(w, r)
	w.Header().Set("Cache-Control", "no-store")

	return openapi.RefreshWebSession200JSONResponse(openapi.RefreshSessionResponse{
		SessionExpiresIn: out.SessionExpiresIn,
	}), nil
}
