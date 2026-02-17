// bff\internal\api\rest\server\accounts_strict.go
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

func (s *Server) PatchAccountLimits(
	ctx context.Context,
	req openapi.PatchAccountLimitsRequestObject,
) (openapi.PatchAccountLimitsResponseObject, error) {
	return s.accounts.PatchLimitsStrict(ctx, req.Id, req.Params, *req.Body)
}

// NUEVO: GET /api/v1/accounts/lookup?account_number=...
func (s *Server) LookupAccountByNumber(
	ctx context.Context,
	req openapi.LookupAccountByNumberRequestObject,
) (openapi.LookupAccountByNumberResponseObject, error) {
	return s.accounts.LookupByNumberStrict(ctx, req.Params)
}

// NUEVO: GET /api/v1/accounts/{id}/statement
func (s *Server) GetAccountStatement(
	ctx context.Context,
	req openapi.GetAccountStatementRequestObject,
) (openapi.GetAccountStatementResponseObject, error) {
	return s.accounts.StatementStrict(ctx, req.Id.String(), req.Params)
}
