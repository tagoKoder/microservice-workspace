package out

import (
	"context"

	"github.com/google/uuid"
	"github.com/tagoKoder/ledger/internal/domain/model"
)

type OutboxRepositoryPort interface {
	Insert(ctx context.Context, e *model.OutboxEvent) error

	// worker
	FetchNextUnpublished(ctx context.Context, limit int) ([]model.OutboxEvent, error)
	MarkPublished(ctx context.Context, ids []uuid.UUID) error
}
