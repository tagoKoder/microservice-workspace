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
	CustomerID     uuid.UUID
	CreatedAt      time.Time
}

type PaymentStep struct {
	ID          uuid.UUID
	PaymentID   uuid.UUID
	Step        string
	State       string
	DetailsJSON string
	AttemptedAt time.Time
}
