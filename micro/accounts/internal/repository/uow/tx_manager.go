package uow

import (
	"context"

	"github.com/tagoKoder/common-kit/pkg/dbx"
	"gorm.io/gorm"
)

type txManager struct{ inner dbx.Runner[UnitOfWork] }

func NewTxManager(writeDB *gorm.DB) TxManager {
	return &txManager{inner: dbx.NewRunner(writeDB, func(tx *gorm.DB) UnitOfWork {
		return &unitOfWork{tx: tx}
	})}
}
func (m *txManager) Do(ctx context.Context, fn func(UnitOfWork) error) error {
	return m.inner.Do(ctx, fn)
}
