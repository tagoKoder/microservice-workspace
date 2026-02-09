// micro\ledger\internal\infra\in\grpc\handler\ledger_handler.go
package handler

import (
	"context"

	in "github.com/tagoKoder/ledger/internal/domain/port/in"
	ledgerpb "github.com/tagoKoder/ledger/internal/genproto/bank/ledgerpayments/v1"
	"github.com/tagoKoder/ledger/internal/infra/in/grpc/mapper"
)

type LedgerHandler struct {
	ledgerpb.UnimplementedLedgerServiceServer
	creaAcc in.CreditAccountUseCase
	listAcc in.ListAccountJournalEntriesUseCase
}

func NewLedgerHandler(creaAcc in.CreditAccountUseCase, listAcc in.ListAccountJournalEntriesUseCase) *LedgerHandler {
	return &LedgerHandler{creaAcc: creaAcc, listAcc: listAcc}
}

func (h *LedgerHandler) CreditAccount(ctx context.Context, req *ledgerpb.CreditAccountRequest) (*ledgerpb.CreditAccountResponse, error) {
	cmd, err := mapper.ToCreditAccountCommand(req)
	if err != nil {
		return nil, err
	}

	res, err := h.creaAcc.CreditAccount(ctx, cmd)
	if err != nil {
		return nil, err
	}

	return mapper.ToCreditAccountResponse(res), nil
}

func (h *LedgerHandler) ListAccountJournalEntries(ctx context.Context, req *ledgerpb.ListAccountJournalEntriesRequest) (*ledgerpb.ListAccountJournalEntriesResponse, error) {
	q, err := mapper.ToListAccountJournalEntriesQuery(req)
	if err != nil {
		return nil, err
	}

	res, err := h.listAcc.ListAccountJournalEntries(ctx, q)
	if err != nil {
		return nil, err
	}

	return mapper.ToListAccountJournalEntriesResponse(res), nil
}
