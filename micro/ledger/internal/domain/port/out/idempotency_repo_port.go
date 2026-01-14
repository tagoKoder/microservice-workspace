package out

import (
	"context"

	"github.com/tagoKoder/ledger/internal/domain/model"
)

type IdempotencyRepositoryPort interface {
	Get(ctx context.Context, key string) (*model.IdempotencyRecord, error)
	Put(ctx context.Context, rec *model.IdempotencyRecord) error
}
