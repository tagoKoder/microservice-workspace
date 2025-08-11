package db

import "context"

type TxManager interface {
	Do(ctx context.Context, fn func(uow UnitOfWork) error) error
}
