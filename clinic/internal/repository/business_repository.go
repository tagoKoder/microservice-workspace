package repository

import (
	"context"

	"github.com/tagoKoder/clinic/internal/domain/model"
)

type BusinessRepository interface {
	Create(ctx context.Context, b *model.Business) error
	GetByID(ctx context.Context, id int64) (*model.Business, error)
	GetByGovernmentID(ctx context.Context, govID string) (*model.Business, error)
	UpdateCore(ctx context.Context, b *model.Business) error // Name, TimeZoneID, etc.
	SoftDelete(ctx context.Context, id int64) error
	Restore(ctx context.Context, id int64) error
}
