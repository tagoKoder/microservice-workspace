package uow

import (
	"github.com/tagoKoder/accounts/internal/module/business/repository"
	gormrepo "github.com/tagoKoder/accounts/internal/module/business/repository"
	"gorm.io/gorm"
)

type queryWork struct {
	db *gorm.DB
}

func (q *queryWork) Businesses() repository.BusinessReadRepository {
	return gormrepo.NewBusinessRepository(q.db)
}

func (q *queryWork) AccountsRep() repository.AccountsRepository {
	return gormrepo.NewAccountsRepository(q.db)
}
