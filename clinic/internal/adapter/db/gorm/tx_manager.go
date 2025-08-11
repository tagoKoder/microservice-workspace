package gorm

import (
	"context"

	"github.com/tagoKoder/clinic/internal/adapter/db"
	"gorm.io/gorm"
)

type txManager struct{ db *gorm.DB }

func NewTxManager(db *gorm.DB) db.TxManager { return &txManager{db: db} }

func (m *txManager) Do(ctx context.Context, fn func(uow db.UnitOfWork) error) error {
	return m.db.WithContext(ctx).Transaction(func(tx *gorm.DB) error {
		uow := &unitOfWork{tx: tx}
		return fn(uow) // nil => COMMIT ; error/panic => ROLLBACK
	})
}
