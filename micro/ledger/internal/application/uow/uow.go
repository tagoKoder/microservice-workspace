package uow

import (
	"context"
)

type UnitOfWorkManager interface {
	DoWrite(ctx context.Context, fn func(WriteRepos) error) error
	DoRead(ctx context.Context, fn func(ReadRepos) error) error
}
