package in

import (
	"context"

	"github.com/google/uuid"
)

type PaymentService interface {
	PostPaymentUseCase
	GetPaymentUseCase
}

type PostPaymentUseCase interface {
	PostPayment(ctx context.Context, cmd PostPaymentCommand) (*PostPaymentResult, error)
}

type GetPaymentUseCase interface {
	GetPayment(ctx context.Context, paymentID uuid.UUID) (*GetPaymentResult, error)
}

type PostPaymentCommand struct {
	IdempotencyKey     string
	SourceAccountID    uuid.UUID
	DestinationAccount uuid.UUID
	Currency           string
	Amount             string // decimal string
	InitiatedBy        string
}

type PostPaymentResult struct {
	PaymentID uuid.UUID
	Status    string
}

type GetPaymentResult struct {
	PaymentID        uuid.UUID
	Status           string
	IdempotencyKey   string
	SourceAccountID  uuid.UUID
	DestAccountID    uuid.UUID
	Currency         string
	Amount           string
	Steps            []PaymentStepView
	CreatedAtRFC3339 string
}

type PaymentStepView struct {
	Step               string
	State              string
	DetailsJSON        string
	AttemptedAtRFC3339 string
}
