package server

import (
	"context"

	openapi "github.com/tagoKoder/bff/internal/api/rest/gen/openapi"
)

func (s *Server) SandboxTopup(
	ctx context.Context,
	req openapi.SandboxTopupRequestObject,
) (openapi.SandboxTopupResponseObject, error) {
	return s.sandbox.TopupStrict(ctx, req.Params, *req.Body)
}
