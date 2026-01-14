package server

import (
	"context"

	openapi "github.com/tagoKoder/bff/internal/api/rest/gen/openapi"
	"github.com/tagoKoder/bff/internal/api/rest/middleware"
)

func (s *Server) StartOnboarding(
	ctx context.Context,
	_ openapi.StartOnboardingRequestObject,
) (openapi.StartOnboardingResponseObject, error) {

	r, ok := middleware.GetHTTPRequest(ctx)
	if !ok || r == nil {
		return openapi.StartOnboarding502JSONResponse(openapi.ErrorResponse{
			Code:    "INTERNAL",
			Message: "missing http request context",
		}), nil
	}

	return s.onboarding.IntentsMultipartStrict(ctx, r)
}

func (s *Server) VerifyOnboardingContact(
	ctx context.Context,
	req openapi.VerifyOnboardingContactRequestObject,
) (openapi.VerifyOnboardingContactResponseObject, error) {
	return s.onboarding.VerifyContactStrict(ctx, *req.Body)
}

func (s *Server) RegisterOnboardingConsents(
	ctx context.Context,
	req openapi.RegisterOnboardingConsentsRequestObject,
) (openapi.RegisterOnboardingConsentsResponseObject, error) {
	return s.onboarding.ConsentsStrict(ctx, *req.Body)
}

func (s *Server) ActivateOnboarding(
	ctx context.Context,
	req openapi.ActivateOnboardingRequestObject,
) (openapi.ActivateOnboardingResponseObject, error) {
	return s.onboarding.ActivateStrict(ctx, *req.Body)
}
