// micro\ledger\internal\domain\port\in\statement.go
package in

import (
	"context"
	"time"

	"github.com/google/uuid"
)

type ListAccountStatementQuery struct {
	AccountID           uuid.UUID
	From                time.Time
	To                  time.Time
	Page                int
	Size                int
	IncludeCounterparty bool
}

type CounterpartyView struct {
	AccountID     string
	AccountNumber string
	DisplayName   string
	AccountType   string
}

type StatementItemView struct {
	JournalID string
	BookedAt  time.Time
	Currency  string

	Direction string // "debit" | "credit"
	Amount    string // decimal string fixed

	Kind string // "transfer" | "bonus" | "other"
	Memo string // external_ref

	CounterpartyAccountID string
	Counterparty          *CounterpartyView
}

type ListAccountStatementResult struct {
	Items []StatementItemView
	Page  int
	Size  int
}

type ListAccountStatementUseCase interface {
	ListAccountStatement(ctx context.Context, q ListAccountStatementQuery) (*ListAccountStatementResult, error)
}
