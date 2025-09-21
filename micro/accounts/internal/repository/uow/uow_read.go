package uow

import (
	"github.com/tagoKoder/accounts/internal/repository"
	gormrepo "github.com/tagoKoder/accounts/internal/repository/impl"
	"gorm.io/gorm"
)

type queryWork struct {
	db *gorm.DB
}

func (q *queryWork) Businesses() repository.BusinessReadRepository {
	return gormrepo.NewBusinessRepository(q.db)
}
