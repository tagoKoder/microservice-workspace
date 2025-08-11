package ports

import "context"

// UnitOfWork: factoría de repos bajo la MISMA transacción.
type UnitOfWork interface {
	Orders() OrderRepository
}

// TxManager: ejecuta fn dentro de una transacción ACID local.
type TxManager interface {
	Do(ctx context.Context, fn func(uow UnitOfWork) error) error
}
