package gorm

import (
	"context"

	"github.com/tagoKoder/clinic/internal/repository"
	repoImpl "github.com/tagoKoder/clinic/internal/repository/impl"
	"gorm.io/gorm"
)

type unitOfWork struct{ tx *gorm.DB }

// Implementa aquí factories de repos por agregado:
func (u *unitOfWork) Businesses() repository.BusinessRepository {
	return repoImpl.NewBusinessRepository(u.tx)
}

// func (u *unitOfWork) Outbox() db.OutboxRepository { return NewOutboxRepository(u.tx) }

// ---- savepoints ----
func (u *unitOfWork) SavePoint(ctx context.Context, name string) error {
	// GORM ejecuta: SAVEPOINT "name"
	return u.tx.WithContext(ctx).SavePoint(name).Error
}

func (u *unitOfWork) RollbackTo(ctx context.Context, name string) error {
	// GORM ejecuta: ROLLBACK TO SAVEPOINT "name"
	return u.tx.WithContext(ctx).RollbackTo(name).Error
}
