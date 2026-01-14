package uow

import (
	"context"

	"github.com/tagoKoder/accounts/internal/module/business/repository"
	gormrepo "github.com/tagoKoder/accounts/internal/module/business/repository"
	"gorm.io/gorm"
)

type unitOfWork struct {
	tx *gorm.DB
}

func (u *unitOfWork) Businesses() repository.BusinessWriteRepository {
	return gormrepo.NewBusinessRepository(u.tx)
}

func (u *unitOfWork) AccountsRep() repository.AccountsRepository {
	return gormrepo.NewAccountsRepository(u.tx)
}

func (u *unitOfWork) SavePoint(ctx context.Context, n string) error {
	return u.tx.WithContext(ctx).SavePoint(n).Error
}
func (u *unitOfWork) RollbackTo(ctx context.Context, n string) error {
	return u.tx.WithContext(ctx).RollbackTo(n).Error
}
