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

// GET /bff/session/csrf
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

	tok := s.deps.CSRF.EnsureToken(w, r)
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

// POST /bff/webauthn/registration/begin
func (s *Server) BeginWebauthnRegistration(
	ctx context.Context,
	req openapi.BeginWebauthnRegistrationRequestObject,
) (openapi.BeginWebauthnRegistrationResponseObject, error) {
	_ = req.Params.XCSRFToken

	identityID, _ := ctx.Value(middleware.CtxIdentity).(string)
	if identityID == "" {
		return openapi.BeginWebauthnRegistration401JSONResponse(openapi.ErrorResponse{
			Code:    "UNAUTHORIZED",
			Message: "missing identity context",
		}), nil
	}

	deviceName := ""
	if req.Body != nil && req.Body.DeviceName != nil {
		deviceName = *req.Body.DeviceName
	}

	out, err := s.deps.Clients.Identity.BeginWebauthnRegistration(ctx, ports.BeginWebauthnRegistrationInput{
		IdentityID: identityID,
		DeviceName: deviceName,
	})
	if err != nil {
		return openapi.BeginWebauthnRegistration502JSONResponse(openapi.ErrorResponse{
			Code:    "UPSTREAM_ERROR",
			Message: "identity service unavailable",
		}), nil
	}

	return openapi.BeginWebauthnRegistration200JSONResponse(openapi.BeginWebauthnRegistrationResponse{
		RequestId:   out.RequestID,
		OptionsJson: out.OptionsJSON,
	}), nil
}

// POST /bff/webauthn/registration/finish
func (s *Server) FinishWebauthnRegistration(
	ctx context.Context,
	req openapi.FinishWebauthnRegistrationRequestObject,
) (openapi.FinishWebauthnRegistrationResponseObject, error) {
	_ = req.Params.XCSRFToken

	if req.Body == nil {
		return openapi.FinishWebauthnRegistration400JSONResponse(openapi.ErrorResponse{
			Code:    "BAD_REQUEST",
			Message: "missing body",
		}), nil
	}

	out, err := s.deps.Clients.Identity.FinishWebauthnRegistration(ctx, ports.FinishWebauthnRegistrationInput{
		RequestID:      req.Body.RequestId,
		CredentialJSON: req.Body.CredentialJson,
	})
	if err != nil {
		return openapi.FinishWebauthnRegistration502JSONResponse(openapi.ErrorResponse{
			Code:    "UPSTREAM_ERROR",
			Message: "identity service unavailable",
		}), nil
	}

	return openapi.FinishWebauthnRegistration200JSONResponse(openapi.WebauthnSuccessResponse{
		Success: out.Success,
	}), nil
}

// POST /bff/webauthn/assertion/begin
func (s *Server) BeginWebauthnAssertion(
	ctx context.Context,
	req openapi.BeginWebauthnAssertionRequestObject,
) (openapi.BeginWebauthnAssertionResponseObject, error) {
	_ = req.Params.XCSRFToken

	identityID, _ := ctx.Value(middleware.CtxIdentity).(string)
	sessionID, _ := ctx.Value(middleware.CtxSession).(string)
	if identityID == "" || sessionID == "" {
		return openapi.BeginWebauthnAssertion401JSONResponse(openapi.ErrorResponse{
			Code:    "UNAUTHORIZED",
			Message: "missing identity/session context",
		}), nil
	}

	out, err := s.deps.Clients.Identity.BeginWebauthnAssertion(ctx, ports.BeginWebauthnAssertionInput{
		IdentityID: identityID,
		SessionID:  sessionID,
	})
	if err != nil {
		return openapi.BeginWebauthnAssertion502JSONResponse(openapi.ErrorResponse{
			Code:    "UPSTREAM_ERROR",
			Message: "identity service unavailable",
		}), nil
	}

	return openapi.BeginWebauthnAssertion200JSONResponse(openapi.BeginWebauthnAssertionResponse{
		RequestId:   out.RequestID,
		OptionsJson: out.OptionsJSON,
	}), nil
}

// POST /bff/webauthn/assertion/finish
func (s *Server) FinishWebauthnAssertion(
	ctx context.Context,
	req openapi.FinishWebauthnAssertionRequestObject,
) (openapi.FinishWebauthnAssertionResponseObject, error) {
	_ = req.Params.XCSRFToken

	sessionID, _ := ctx.Value(middleware.CtxSession).(string)
	if sessionID == "" {
		return openapi.FinishWebauthnAssertion401JSONResponse(openapi.ErrorResponse{
			Code:    "UNAUTHORIZED",
			Message: "missing session context",
		}), nil
	}
	if req.Body == nil {
		return openapi.FinishWebauthnAssertion400JSONResponse(openapi.ErrorResponse{
			Code:    "BAD_REQUEST",
			Message: "missing body",
		}), nil
	}

	out, err := s.deps.Clients.Identity.FinishWebauthnAssertion(ctx, ports.FinishWebauthnAssertionInput{
		SessionID:      sessionID,
		RequestID:      req.Body.RequestId,
		CredentialJSON: req.Body.CredentialJson,
	})
	if err != nil {
		return openapi.FinishWebauthnAssertion502JSONResponse(openapi.ErrorResponse{
			Code:    "UPSTREAM_ERROR",
			Message: "identity service unavailable",
		}), nil
	}

	return openapi.FinishWebauthnAssertion200JSONResponse(openapi.WebauthnSuccessResponse{
		Success: out.Success,
	}), nil
}
