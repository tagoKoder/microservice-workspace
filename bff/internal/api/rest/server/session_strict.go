// bff\internal\api\rest\server\session_strict.go
package server

import (
	"context"

	openapi "github.com/tagoKoder/bff/internal/api/rest/gen/openapi"
)

func (s *Server) GetCurrentSession(
	ctx context.Context,
	_ openapi.GetCurrentSessionRequestObject,
) (openapi.GetCurrentSessionResponseObject, error) {
	return s.session.WhoamiStrict(ctx)
}
