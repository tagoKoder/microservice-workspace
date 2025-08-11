package db

import (
	"context"

	"github.com/tagoKoder/clinic/internal/repository"
)

// UnitOfWork y TxManager ya los tenías:
type UnitOfWork interface {
	// Agrega aquí tus repos por agregado (no por tabla):
	Businesses() repository.BusinessRepository
	// Outbox() OutboxRepository // si usas outbox
	// Savepoints (rollback parcial dentro de la misma transacción)
	SavePoint(ctx context.Context, name string) error
	RollbackTo(ctx context.Context, name string) error
}
