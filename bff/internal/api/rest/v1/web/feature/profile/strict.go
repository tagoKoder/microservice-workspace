package profile

import (
	"context"

	openapi "github.com/tagoKoder/bff/internal/api/rest/gen/openapi"
	"github.com/tagoKoder/bff/internal/api/rest/middleware"
	"github.com/tagoKoder/bff/internal/client/ports"
	"github.com/tagoKoder/bff/internal/security"
)

func (h *Handler) PatchStrict(
	ctx context.Context,
	params openapi.UpdateProfileParams,
	body openapi.PatchProfileRequest,
) (openapi.UpdateProfileResponseObject, error) {
	_ = params.XCSRFToken // validado arriba
	corrId := security.CorrelationID(ctx)
	customerID, _ := ctx.Value(middleware.CtxCustomer).(string)
	if customerID == "" {
		return openapi.UpdateProfile403JSONResponse(openapi.ErrorResponse{
			Code:    "FORBIDDEN",
			Message: "missing customer context",
			Details: &map[string]interface{}{"correlation_id": corrId},
		}), nil
	}

	var pref *ports.PreferencesPatch
	if body.Preferences != nil && len(*body.Preferences) > 0 {
		p0 := (*body.Preferences)[0]
		ch := mapPref(p0.Channel)
		pref = &ports.PreferencesPatch{
			Channel: &ch,
			OptIn:   &p0.OptIn,
		}
	}

	_, err := h.clients.Accounts.PatchCustomer(ctx, ports.PatchCustomerInput{
		ID:       customerID,
		FullName: body.FullName,
		Contact: &ports.CustomerContactPatch{
			Email: body.Email,
			Phone: body.Phone,
		},
		Preferences: pref,
	})
	if err != nil {
		return openapi.UpdateProfile502JSONResponse(openapi.ErrorResponse{
			Code:    "UPSTREAM_ERROR",
			Message: "accounts service unavailable",
			Details: &map[string]interface{}{"correlation_id": corrId},
		}), nil
	}

	return openapi.UpdateProfile200JSONResponse{
		Body: openapi.PatchProfileResponse{Updated: true},
		Headers: openapi.UpdateProfile200ResponseHeaders{
			XCorrelationId: corrId,
		},
	}, nil
}

func mapPref(s openapi.PatchProfileRequestPreferencesChannel) ports.PreferenceChannel {
	switch s {
	case openapi.Email:
		return ports.PrefEmail
	case openapi.Sms:
		return ports.PrefSMS
	case openapi.Push:
		return ports.PrefPush
	default:
		return ports.PrefEmail
	}
}
