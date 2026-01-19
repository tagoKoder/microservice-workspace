package payments

import (
	"context"
	"strconv"
	"strings"
	"time"

	openapi "github.com/tagoKoder/bff/internal/api/rest/gen/openapi"
	"github.com/tagoKoder/bff/internal/api/rest/middleware"
	"github.com/tagoKoder/bff/internal/client/ports"
)

func (h *Handler) CreateBeneficiaryStrict(
	ctx context.Context,
	params openapi.CreateBeneficiaryParams,
	body openapi.CreateBeneficiaryRequest,
) (openapi.CreateBeneficiaryResponseObject, error) {
	_ = params.XCSRFToken // ya validado por strict + CSRF middleware

	// MVP “beneficiary”: sin puerto específico en tu ports (no hay BeneficiariesPort).
	// Al menos validamos que el usuario tenga customer en contexto y que el destino sea una cuenta visible.
	customerID, _ := ctx.Value(middleware.CtxCustomer).(string)
	if customerID == "" {
		return openapi.CreateBeneficiary403JSONResponse(openapi.ErrorResponse{
			Code:    "FORBIDDEN",
			Message: "missing customer context",
		}), nil
	}

	accs, err := h.clients.Accounts.ListAccounts(ctx, ports.ListAccountsInput{CustomerID: customerID})
	if err != nil {
		return openapi.CreateBeneficiary502JSONResponse(openapi.ErrorResponse{
			Code:    "UPSTREAM_ERROR",
			Message: "accounts service unavailable",
		}), nil
	}

	ok := false
	for _, a := range accs.Accounts {
		if a.ID == body.DestinationAccount {
			ok = true
			break
		}
	}
	if !ok {
		return openapi.CreateBeneficiary400JSONResponse(openapi.ErrorResponse{
			Code:    "BAD_REQUEST",
			Message: "destination_account not allowed for this customer",
		}), nil
	}

	resp := openapi.CreateBeneficiaryResponse{BeneficiaryId: body.DestinationAccount}
	return openapi.CreateBeneficiary201JSONResponse(resp), nil
}

func (h *Handler) CreatePaymentStrict(
	ctx context.Context,
	params openapi.ExecutePaymentParams,
	body openapi.CreatePaymentRequest,
) (openapi.ExecutePaymentResponseObject, error) {
	customerID, _ := ctx.Value(middleware.CtxCustomer).(string)
	if customerID == "" {
		return openapi.ExecutePayment403JSONResponse(openapi.ErrorResponse{
			Code:    "FORBIDDEN",
			Message: "missing customer context",
		}), nil
	}

	sub, _ := ctx.Value(middleware.CtxSubject).(string)
	if sub == "" {
		return openapi.ExecutePayment401JSONResponse(openapi.ErrorResponse{
			Code:    "UNAUTHORIZED",
			Message: "missing subject context",
		}), nil
	}

	amtFloat, err := strconv.ParseFloat(body.Amount, 64)
	if err != nil || amtFloat <= 0 {
		return openapi.ExecutePayment400JSONResponse(openapi.ErrorResponse{
			Code:    "BAD_REQUEST",
			Message: "invalid amount",
		}), nil
	}

	// Validación de cuentas y límites (micro Accounts internal)
	val, err := h.clients.Accounts.ValidateAccountsAndLimits(ctx, ports.ValidateAccountsAndLimitsInput{
		SourceAccountID:      body.SourceAccount,
		DestinationAccountID: body.DestinationAccount,
		Currency:             body.Currency,
		Amount:               amtFloat,
	})
	if err != nil {
		return openapi.ExecutePayment502JSONResponse(openapi.ErrorResponse{
			Code:    "UPSTREAM_ERROR",
			Message: "accounts validation unavailable",
		}), nil
	}
	if !val.OK {
		details := map[string]any{}
		if val.Reason != nil {
			details["reason"] = *val.Reason
		}
		return openapi.ExecutePayment400JSONResponse(openapi.ErrorResponse{
			Code:    "VALIDATION_FAILED",
			Message: "payment validation failed",
			Details: &details,
		}), nil
	}

	// Ejecuta pago (micro LedgerPayments)
	out, err := h.clients.LedgerPayments.PostPayment(ctx, ports.PostPaymentInput{
		IdempotencyKey:       params.IdempotencyKey,
		SourceAccountID:      body.SourceAccount,
		DestinationAccountID: body.DestinationAccount,
		Currency:             body.Currency,
		Amount:               body.Amount,
		InitiatedBy:          sub,
	})
	if err != nil {
		return openapi.ExecutePayment502JSONResponse(openapi.ErrorResponse{
			Code:    "UPSTREAM_ERROR",
			Message: "ledgerpayments service unavailable",
		}), nil
	}

	resp := openapi.CreatePaymentResponse{
		PaymentId: out.PaymentID,
		Status:    mapPaymentStatus(out.Status),
	}
	return openapi.ExecutePayment201JSONResponse(resp), nil
}

func (h *Handler) GetPaymentStrict(
	ctx context.Context,
	id string,
) (openapi.GetPaymentStatusResponseObject, error) {
	customerID, _ := ctx.Value(middleware.CtxCustomer).(string)
	if customerID == "" {
		return openapi.GetPaymentStatus403JSONResponse(openapi.ErrorResponse{
			Code:    "FORBIDDEN",
			Message: "missing customer context",
		}), nil
	}

	out, err := h.clients.LedgerPayments.GetPayment(ctx, ports.GetPaymentInput{PaymentID: id})
	if err != nil {
		return openapi.GetPaymentStatus502JSONResponse(openapi.ErrorResponse{
			Code:    "UPSTREAM_ERROR",
			Message: "ledgerpayments service unavailable",
		}), nil
	}

	createdAt, err := time.Parse(time.RFC3339, out.CreatedAtRFC3339)
	if err != nil {
		return openapi.GetPaymentStatus502JSONResponse(openapi.ErrorResponse{
			Code:    "UPSTREAM_ERROR",
			Message: "invalid created_at from upstream",
		}), nil
	}

	steps := make([]openapi.PaymentStep, 0, len(out.Steps))
	for _, s := range out.Steps {
		at, err := time.Parse(time.RFC3339, s.AttemptedAtRFC3339)
		if err != nil {
			return openapi.GetPaymentStatus502JSONResponse(openapi.ErrorResponse{
				Code:    "UPSTREAM_ERROR",
				Message: "invalid attempted_at from upstream",
			}), nil
		}
		var details *string
		if strings.TrimSpace(s.DetailsJSON) != "" {
			d := s.DetailsJSON
			details = &d
		}
		steps = append(steps, openapi.PaymentStep{
			Step:        s.Step,
			State:       s.State,
			AttemptedAt: at,
			DetailsJson: details,
		})
	}

	resp := openapi.GetPaymentResponse{
		PaymentId:          out.PaymentID,
		Status:             out.Status,
		IdempotencyKey:     out.IdempotencyKey,
		SourceAccount:      out.SourceAccountID,
		DestinationAccount: out.DestinationAccountID,
		Currency:           out.Currency,
		Amount:             out.Amount,
		Steps:              steps,
		CreatedAt:          createdAt,
	}

	return openapi.GetPaymentStatus200JSONResponse(resp), nil
}

func mapPaymentStatus(s string) openapi.CreatePaymentResponseStatus {
	switch strings.ToLower(strings.TrimSpace(s)) {
	case "posted":
		return openapi.CreatePaymentResponseStatus("posted")
	case "pending":
		return openapi.CreatePaymentResponseStatus("pending")
	case "rejected":
		return openapi.CreatePaymentResponseStatus("rejected")
	case "failed":
		return openapi.CreatePaymentResponseStatus("failed")
	default:
		// fallback seguro
		return openapi.CreatePaymentResponseStatus("pending")
	}
}
