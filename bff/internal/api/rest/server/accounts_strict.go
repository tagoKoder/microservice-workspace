package server

import (
	"context"

	openapi "github.com/tagoKoder/bff/internal/api/rest/gen/openapi"
)

func (s *Server) GetAccountsOverview(
	ctx context.Context,
	_ openapi.GetAccountsOverviewRequestObject,
) (openapi.GetAccountsOverviewResponseObject, error) {
	return s.accounts.OverviewStrict(ctx)
}

func (s *Server) GetAccountActivity(
	ctx context.Context,
	req openapi.GetAccountActivityRequestObject,
) (openapi.GetAccountActivityResponseObject, error) {
	return s.accounts.ActivityStrict(ctx, req.Id, req.Params)
}

func (s *Server) PatchAccountLimits(
	ctx context.Context,
	req openapi.PatchAccountLimitsRequestObject,
) (openapi.PatchAccountLimitsResponseObject, error) {
	return s.accounts.PatchLimitsStrict(ctx, req.Id, req.Params, *req.Body)
}
