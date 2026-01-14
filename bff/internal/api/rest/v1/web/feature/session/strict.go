package session

import (
	"context"

	openapi "github.com/tagoKoder/bff/internal/api/rest/gen/openapi"
	"github.com/tagoKoder/bff/internal/api/rest/middleware"
)

func (h *Handler) WhoamiStrict(ctx context.Context) (openapi.GetCurrentSessionResponseObject, error) {
	sub, _ := ctx.Value(middleware.CtxSubject).(string)
	if sub == "" {
		return openapi.GetCurrentSession401JSONResponse(openapi.ErrorResponse{
			Code:    "UNAUTHORIZED",
			Message: "missing subject context",
		}), nil
	}

	customerID, _ := ctx.Value(middleware.CtxCustomer).(string)
	status, _ := ctx.Value(middleware.CtxUserState).(string)
	roles, _ := ctx.Value(middleware.CtxRoles).([]string)
	mfaReq, _ := ctx.Value(middleware.CtxMfaRequired).(bool)
	mfaVer, _ := ctx.Value(middleware.CtxMfaVerified).(bool)

	var custPtr *string
	if customerID != "" {
		custPtr = &customerID
	}

	resp := openapi.WhoamiResponse{
		SubjectIdOidc: sub,
		CustomerId:    custPtr,
		UserStatus:    status,
		Roles:         append([]string{}, roles...),
		MfaRequired:   mfaReq,
		MfaVerified:   mfaVer,
	}

	return openapi.GetCurrentSession200JSONResponse(resp), nil
}
