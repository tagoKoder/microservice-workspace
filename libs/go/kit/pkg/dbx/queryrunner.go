package dbx

import (
	"context"

	"gorm.io/gorm"
)

type QueryRunner[T any] interface {
	Do(context.Context, func(T) error) error
}

type queryRunner[T any] struct {
	db      *gorm.DB
	factory func(*gorm.DB) T
}

func NewQueryRunner[T any](db *gorm.DB, factory func(*gorm.DB) T) QueryRunner[T] {
	return &queryRunner[T]{db: db, factory: factory}
}
func (r *queryRunner[T]) Do(ctx context.Context, fn func(T) error) error {
	return fn(r.factory(r.db.WithContext(ctx))) // no BEGIN/COMMIT
}
