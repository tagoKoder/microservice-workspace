package uow

import (
	"context"

	"github.com/tagoKoder/clinic/internal/repository"
	gormrepo "github.com/tagoKoder/clinic/internal/repository/impl"
	"gorm.io/gorm"
)

type unitOfWork struct {
	tx *gorm.DB
}

func (u *unitOfWork) Businesses() repository.BusinessWriteRepository {
	return gormrepo.NewBusinessRepository(u.tx)
}
func (u *unitOfWork) SavePoint(ctx context.Context, n string) error {
	return u.tx.WithContext(ctx).SavePoint(n).Error
}
func (u *unitOfWork) RollbackTo(ctx context.Context, n string) error {
	return u.tx.WithContext(ctx).RollbackTo(n).Error
}
