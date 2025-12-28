package out

import (
	"context"

	"github.com/google/uuid"
	"github.com/tagoKoder/ledger/internal/domain/model"
)

type PaymentRepositoryPort interface {
	FindById(ctx context.Context, id uuid.UUID) (*model.Payment, error)
	FindByIdempotencyKey(ctx context.Context, key string) (*model.Payment, error)
	Insert(ctx context.Context, p *model.Payment) error
	InsertStep(ctx context.Context, step *model.PaymentStep) error
	UpdateStatus(ctx context.Context, id uuid.UUID, status string) error
	ListSteps(ctx context.Context, paymentID uuid.UUID) ([]model.PaymentStep, error)
}
