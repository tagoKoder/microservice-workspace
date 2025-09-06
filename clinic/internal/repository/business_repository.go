package repository

import (
	"context"

	"github.com/tagoKoder/clinic/internal/domain/model"
)

type BusinessReadRepository interface {
	GetByID(ctx context.Context, id int64) (*model.Business, error)
	GetByGovernmentID(ctx context.Context, govID string) (*model.Business, error)
	// List(...) ...
}

type BusinessWriteRepository interface {
	Create(ctx context.Context, b *model.Business) error
	UpdateCore(ctx context.Context, b *model.Business) error
	SoftDelete(ctx context.Context, id int64) error
	Restore(ctx context.Context, id int64) error
}

type BusinessRepository interface { // opcional (implementación cumple ambas)
	BusinessReadRepository
	BusinessWriteRepository
}
