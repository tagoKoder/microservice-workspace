package handler

import (
	"context"

	in "github.com/tagoKoder/ledger/internal/domain/port/in"
	"github.com/tagoKoder/ledger/internal/infra/in/grpc/mapper"
	ledgerpb "github.com/tagoKoder/ledger/proto/gen/ledgerpayments/v1"
)

type LedgerHandler struct {
	ledgerpb.UnimplementedLedgerServiceServer
	creaAcc   in.CreditAccountUseCase
	listAcc   in.ListAccountJournalEntriesUseCase
	createMan in.CreateManualJournalEntryUseCase
}

func NewLedgerHandler(creaAcc in.CreditAccountUseCase, listAcc in.ListAccountJournalEntriesUseCase, createMan in.CreateManualJournalEntryUseCase) *LedgerHandler {
	return &LedgerHandler{creaAcc: creaAcc, listAcc: listAcc, createMan: createMan}
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

	return mapper.ToCreditAccountResponse(&res), nil
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

	return mapper.ToListAccountJournalEntriesResponse(&res), nil
}

func (h *LedgerHandler) CreateManualJournalEntry(ctx context.Context, req *ledgerpb.CreateManualJournalEntryRequest) (*ledgerpb.CreateManualJournalEntryResponse, error) {
	cmd := mapper.ToCreateManualJournalEntryCommand(req)
	res, err := h.createMan.CreateManualJournalEntry(ctx, cmd)
	if err != nil {
		return nil, err
	}

	return mapper.ToCreateManualJournalEntryResponse(res), nil
}
