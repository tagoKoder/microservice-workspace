package uow

import (
	"context"

	"github.com/tagoKoder/accounts/internal/module/business/repository"
)

type UnitOfWork interface { // WRITE
	Businesses() repository.BusinessWriteRepository
	AccountsRep() repository.AccountsRepository
	SavePoint(ctx context.Context, name string) error
	RollbackTo(ctx context.Context, name string) error
}

type QueryWork interface { // READ
	Businesses() repository.BusinessReadRepository
	AccountsRep() repository.AccountsRepository
}

type TxManager interface {
	Do(ctx context.Context, fn func(uow UnitOfWork) error) error
}

type QueryManager interface {
	Do(ctx context.Context, fn func(q QueryWork) error) error
}
