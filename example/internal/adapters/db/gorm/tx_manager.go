package gormdb

import (
	"context"

	"github.com/tagoKoder/clinic/internal/ports"
	"gorm.io/gorm"
)

type txManager struct{ db *gorm.DB }

func NewTxManager(db *gorm.DB) ports.TxManager { return &txManager{db: db} }

func (m *txManager) Do(ctx context.Context, fn func(uow ports.UnitOfWork) error) error {
	return m.db.WithContext(ctx).Transaction(func(tx *gorm.DB) error {
		uow := &unitOfWork{tx: tx}
		return fn(uow)
	})
}

type unitOfWork struct{ tx *gorm.DB }

func (u *unitOfWork) Orders() ports.OrderRepository {
	return NewOrderRepository(u.tx)
}
