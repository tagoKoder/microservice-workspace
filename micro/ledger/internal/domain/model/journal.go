package model

import (
	"time"

	"github.com/google/uuid"
	"github.com/shopspring/decimal"
)

type JournalStatus string

const (
	JournalPosted   JournalStatus = "posted"
	JournalReversed JournalStatus = "reversed"
	JournalVoid     JournalStatus = "void"
)

type JournalEntry struct {
	ID          uuid.UUID
	ExternalRef string
	BookedAt    time.Time
	CreatedBy   string
	Status      JournalStatus
	Currency    string
	Lines       []EntryLine
}

type EntryLine struct {
	ID              uuid.UUID
	JournalID       uuid.UUID
	GLAccountID     uuid.UUID
	GLAccountCode   string // para vistas
	CounterpartyRef *string
	Debit           decimal.Decimal
	Credit          decimal.Decimal
}
