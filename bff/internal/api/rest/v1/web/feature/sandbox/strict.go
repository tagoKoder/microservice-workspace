package sandbox

import (
	"context"

	openapi "github.com/tagoKoder/bff/internal/api/rest/gen/openapi"
	"github.com/tagoKoder/bff/internal/api/rest/middleware"
	"github.com/tagoKoder/bff/internal/client/ports"
)

func (h *Handler) TopupStrict(
	ctx context.Context,
	params openapi.SandboxTopupParams,
	body openapi.SandboxTopupRequest,
) (openapi.SandboxTopupResponseObject, error) {
	_ = params.XCSRFToken // validado por CSRF middleware + strict header

	if !hasRoleCtx(ctx, "admin") {
		return openapi.SandboxTopup403JSONResponse(openapi.ErrorResponse{
			Code:    "FORBIDDEN",
			Message: "admin role required",
		}), nil
	}

	sub, _ := ctx.Value(middleware.CtxSubject).(string)
	if sub == "" {
		return openapi.SandboxTopup401JSONResponse(openapi.ErrorResponse{
			Code:    "UNAUTHORIZED",
			Message: "missing subject context",
		}), nil
	}

	var ext *string
	if body.Memo != nil && *body.Memo != "" {
		ext = body.Memo
	}

	out, err := h.clients.LedgerPayments.CreditAccount(ctx, ports.CreditAccountInput{
		IdempotencyKey: params.IdempotencyKey,
		AccountID:      body.AccountId,
		Currency:       body.Currency,
		Amount:         body.Amount,
		InitiatedBy:    sub,
		ExternalRef:    ext,
	})
	if err != nil {
		return openapi.SandboxTopup502JSONResponse(openapi.ErrorResponse{
			Code:    "UPSTREAM_ERROR",
			Message: "ledger service unavailable",
		}), nil
	}

	resp := openapi.SandboxTopupResponse{
		JournalId: out.JournalID,
		Status:    out.Status,
	}
	return openapi.SandboxTopup201JSONResponse(resp), nil
}

func hasRoleCtx(ctx context.Context, role string) bool {
	roles, _ := ctx.Value(middleware.CtxRoles).([]string)
	for _, r := range roles {
		if r == role {
			return true
		}
	}
	return false
}
