package ports

import (
	"context"

	"github.com/tagoKoder/clinic/internal/domain"
)

type OrderRepository interface {
	Save(ctx context.Context, o *domain.Order) error
	GetByID(ctx context.Context, id string) (*domain.Order, error)
}
