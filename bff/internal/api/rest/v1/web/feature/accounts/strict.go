package accounts

import (
	"context"
	"strconv"
	"time"

	openapi "github.com/tagoKoder/bff/internal/api/rest/gen/openapi"
	"github.com/tagoKoder/bff/internal/api/rest/middleware"
	"github.com/tagoKoder/bff/internal/client/ports"
	"github.com/tagoKoder/bff/internal/security"
)

func (h *Handler) OverviewStrict(ctx context.Context) (openapi.GetAccountsOverviewResponseObject, error) {
	corrId := security.CorrelationID(ctx)
	customerID, _ := ctx.Value(middleware.CtxCustomer).(string)
	if customerID == "" {
		return openapi.GetAccountsOverview403JSONResponse(openapi.ErrorResponse{
			Code:    "FORBIDDEN",
			Message: "missing customer context",
			Details: &map[string]interface{}{"correlation_id": corrId},
		}), nil
	}

	out, err := h.clients.Accounts.ListAccounts(ctx, ports.ListAccountsInput{CustomerID: customerID})
	if err != nil {
		return openapi.GetAccountsOverview502JSONResponse(openapi.ErrorResponse{
			Code:    "UPSTREAM_ERROR",
			Message: "service unavailable",
			Details: &map[string]interface{}{"correlation_id": corrId},
		}), nil
	}

	resp := openapi.AccountsOverviewResponse{Accounts: make([]openapi.AccountItem, 0, len(out.Accounts))}
	for _, a := range out.Accounts {
		bal := openapi.AccountBalances{Ledger: "0.00", Available: "0.00", Hold: "0.00"}
		if a.Balances != nil {
			bal = openapi.AccountBalances{
				Ledger:    formatMoney(a.Balances.Ledger),
				Available: formatMoney(a.Balances.Available),
				Hold:      formatMoney(a.Balances.Hold),
			}
		}
		resp.Accounts = append(resp.Accounts, openapi.AccountItem{
			Id:          a.ID,
			ProductType: a.ProductType,
			Currency:    a.Currency,
			Status:      a.Status,
			Balances:    bal,
		})
	}

	return openapi.GetAccountsOverview200JSONResponse{
		Body: resp,
		Headers: openapi.GetAccountsOverview200ResponseHeaders{
			XCorrelationId: corrId,
		},
	}, nil
}

func (h *Handler) ActivityStrict(
	ctx context.Context,
	accountID string,
	params openapi.GetAccountActivityParams,
) (openapi.GetAccountActivityResponseObject, error) {
	corrId := security.CorrelationID(ctx)
	customerID, _ := ctx.Value(middleware.CtxCustomer).(string)
	if customerID == "" {
		return openapi.GetAccountActivity403JSONResponse(openapi.ErrorResponse{
			Code:    "FORBIDDEN",
			Message: "missing customer context",
			Details: &map[string]interface{}{"correlation_id": corrId},
		}), nil
	}

	// Page/Size tipados por OpenAPI (pointers)
	page := int32(1)
	size := int32(50)
	if params.Page != nil && *params.Page > 0 {
		page = int32(*params.Page)
	}
	if params.Size != nil && *params.Size > 0 {
		if *params.Size > 200 {
			size = 200
		} else {
			size = int32(*params.Size)
		}
	}

	out, err := h.clients.LedgerPayments.ListAccountJournalEntries(ctx, ports.ListAccountJournalEntriesInput{
		AccountID: accountID,
		Page:      page,
		Size:      size,
	})
	if err != nil {
		return openapi.GetAccountActivity502JSONResponse(openapi.ErrorResponse{
			Code:    "UPSTREAM_ERROR",
			Message: "ledger service unavailable",
			Details: &map[string]interface{}{"correlation_id": corrId},
		}), nil
	}

	items := make([]openapi.AccountActivityItem, 0, len(out.Entries))
	for _, e := range out.Entries {
		t, err := time.Parse(time.RFC3339, e.BookedAtRFC3339)
		if err != nil {
			return openapi.GetAccountActivity502JSONResponse(openapi.ErrorResponse{
				Code:    "UPSTREAM_ERROR",
				Message: "invalid booked_at from upstream",
				Details: &map[string]interface{}{"correlation_id": corrId},
			}), nil
		}
		items = append(items, openapi.AccountActivityItem{
			JournalId: e.JournalID,
			BookedAt:  t,
			Memo:      nil,
		})
	}
	total := len(items)
	resp := openapi.AccountActivityResponse{
		Items: items,
		Page:  int(page),
		Size:  int(size),
		Total: &total,
	}

	return openapi.GetAccountActivity200JSONResponse{
		Body: resp,
		Headers: openapi.GetAccountActivity200ResponseHeaders{
			XCorrelationId: corrId,
		},
	}, nil
}

func (h *Handler) PatchLimitsStrict(
	ctx context.Context,
	accountID string,
	params openapi.PatchAccountLimitsParams,
	body openapi.PatchAccountLimitsHttpRequest,
) (openapi.PatchAccountLimitsResponseObject, error) {

	_ = params.XCSRFToken
	//idem := params.IdempotencyKey
	corrId := security.CorrelationID(ctx)

	// parse opcionales (string decimal) a float64 si tu proto usa float
	var dailyOut *float64
	if body.DailyOut != nil {
		v, err := strconv.ParseFloat(*body.DailyOut, 64)
		if err != nil || v < 0 {
			return openapi.PatchAccountLimits400JSONResponse(openapi.ErrorResponse{
				Code:    "BAD_REQUEST",
				Message: "invalid daily_out value",
				Details: &map[string]interface{}{"correlation_id": corrId},
			}), nil
		}
		dailyOut = &v
	}

	var dailyIn *float64
	if body.DailyIn != nil {
		v, err := strconv.ParseFloat(*body.DailyIn, 64)
		if err != nil || v < 0 {
			return openapi.PatchAccountLimits400JSONResponse(openapi.ErrorResponse{
				Code:    "BAD_REQUEST",
				Message: "invalid daily_in value",
				Details: &map[string]interface{}{"correlation_id": corrId},
			}), nil
		}
		dailyIn = &v
	}

	out, err := h.clients.Accounts.PatchAccountLimits(ctx, ports.PatchAccountLimitsInput{
		//IdempotencyKey: idem, // añade este campo en ports si aún no existe
		ID:       accountID,
		DailyOut: dailyOut,
		DailyIn:  dailyIn,
	})
	if err != nil {
		// si tu micro devuelve “idempotency mismatch” mapear a 409
		return openapi.PatchAccountLimits502JSONResponse(openapi.ErrorResponse{
			Code: "UPSTREAM_ERROR", Message: "accounts service unavailable",
			Details: &map[string]interface{}{"correlation_id": corrId},
		}), nil
	}

	return openapi.PatchAccountLimits200JSONResponse{
		Body: openapi.PatchAccountLimitsHttpResponse{
			AccountId: out.AccountID,
			DailyOut:  formatMoney(out.DailyOut),
			DailyIn:   formatMoney(out.DailyIn),
		},
		Headers: openapi.PatchAccountLimits200ResponseHeaders{
			XCorrelationId: corrId,
		},
	}, nil
}

func formatMoney(v float64) string {
	return strconv.FormatFloat(v, 'f', 2, 64)
}
