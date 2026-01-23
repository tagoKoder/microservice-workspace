package session

import (
	"context"

	openapi "github.com/tagoKoder/bff/internal/api/rest/gen/openapi"
	"github.com/tagoKoder/bff/internal/api/rest/middleware"
	"github.com/tagoKoder/bff/internal/security"
)

func (h *Handler) WhoamiStrict(ctx context.Context) (openapi.GetCurrentSessionResponseObject, error) {
	sub, _ := ctx.Value(middleware.CtxSubject).(string)
	corrId := security.CorrelationID(ctx)
	if sub == "" {
		return openapi.GetCurrentSession401JSONResponse(openapi.ErrorResponse{
			Code:    "UNAUTHORIZED",
			Message: "missing subject context",
			Details: &map[string]interface{}{"correlation_id": corrId},
		}), nil
	}

	customerID, _ := ctx.Value(middleware.CtxCustomer).(string)
	status, _ := ctx.Value(middleware.CtxUserState).(string)
	roles, _ := ctx.Value(middleware.CtxRoles).([]string)

	var custPtr *string
	if customerID != "" {
		custPtr = &customerID
	}

	resp := openapi.WhoamiResponse{
		SubjectIdOidc: sub,
		CustomerId:    custPtr,
		UserStatus:    status,
		Roles:         append([]string{}, roles...),
	}

	return openapi.GetCurrentSession200JSONResponse{
		Body: resp,
		Headers: openapi.GetCurrentSession200ResponseHeaders{
			XCorrelationId: corrId,
		},
	}, nil
}
