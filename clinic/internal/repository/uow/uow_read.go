package uow

import (
	"github.com/tagoKoder/clinic/internal/repository"
	gormrepo "github.com/tagoKoder/clinic/internal/repository/impl"
	"gorm.io/gorm"
)

type queryWork struct {
	db *gorm.DB
}

func (q *queryWork) Businesses() repository.BusinessReadRepository {
	return gormrepo.NewBusinessRepository(q.db)
}
