package in

import (
	"context"

	"github.com/google/uuid"
)

type LedgerAppService interface {
	CreditAccountUseCase
	ListAccountJournalEntriesUseCase
	ListAccountStatementUseCase
}

type CreditAccountUseCase interface {
	CreditAccount(ctx context.Context, cmd CreditAccountCommand) (*CreditAccountResult, error)
}

type ListAccountJournalEntriesUseCase interface {
	ListAccountJournalEntries(ctx context.Context, q ListAccountJournalEntriesQuery) (*ListAccountJournalEntriesResult, error)
}

type CreditAccountCommand struct {
	IdempotencyKey string
	AccountID      uuid.UUID
	Currency       string
	Amount         string
	InitiatedBy    string
	ExternalRef    string
}

type CreditAccountResult struct {
	JournalID uuid.UUID
	Status    string
}

type ListAccountJournalEntriesQuery struct {
	AccountID   uuid.UUID
	FromRFC3339 string
	ToRFC3339   string
	Page        int
	Size        int
}

type ListAccountJournalEntriesResult struct {
	Entries []JournalEntryView
	Page    int
	Size    int
}

type JournalEntryView struct {
	JournalID       string
	Currency        string
	Status          string
	BookedAtRFC3339 string
	ExternalRef     string
	Lines           []JournalLineView
}

type JournalLineView struct {
	GLAccountCode   string
	CounterpartyRef string
	Debit           string
	Credit          string
}
