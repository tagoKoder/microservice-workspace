// micro/ledger/internal/domain/model/payment.go
package model

import (
	"time"

	"github.com/google/uuid"
	"github.com/shopspring/decimal"
)

type PaymentStatus string

const (
	PaymentPending    PaymentStatus = "pending"
	PaymentProcessing PaymentStatus = "processing"
	PaymentPosted     PaymentStatus = "posted"
	PaymentFailed     PaymentStatus = "failed"
)

type Payment struct {
	ID             uuid.UUID
	IdempotencyKey string
	SourceAccount  uuid.UUID
	DestAccount    uuid.UUID
	Amount         decimal.Decimal
	Currency       string
	Status         PaymentStatus

	// OJO: tu entity tiene CustomerID *uuid.UUID + not null.
	// Si DB es NOT NULL, aquí lo dejamos puntero pero el service debe garantizar != nil.
	CustomerID *uuid.UUID

	// SAGA / HOLD / LEDGER
	HoldID        *uuid.UUID
	JournalID     *uuid.UUID
	CorrelationID string

	CreatedAt time.Time
	UpdatedAt time.Time
}

type PaymentStep struct {
	ID          uuid.UUID
	PaymentID   uuid.UUID
	Step        string
	State       string
	DetailsJSON string
	AttemptedAt time.Time
}
