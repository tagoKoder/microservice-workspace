package dbx

import (
	"context"

	"gorm.io/gorm"
)

type Runner[T any] interface {
	Do(ctx context.Context, fn func(uow T) error) error
}

type runner[T any] struct {
	db      *gorm.DB
	factory func(*gorm.DB) T
}

func NewRunner[T any](db *gorm.DB, factory func(*gorm.DB) T) Runner[T] {
	return &runner[T]{db: db, factory: factory}
}

func (r *runner[T]) Do(ctx context.Context, fn func(uow T) error) error {
	return r.db.WithContext(ctx).Transaction(func(tx *gorm.DB) error {
		return fn(r.factory(tx)) // nil => COMMIT ; error/panic => ROLLBACK
	})
}
