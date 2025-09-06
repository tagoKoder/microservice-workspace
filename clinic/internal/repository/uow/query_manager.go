package uow

import (
	"context"

	"github.com/tagoKoder/common-kit/pkg/dbx"
	"gorm.io/gorm"
)

type queryManager struct {
	inner dbx.QueryRunner[QueryWork]
}

func NewQueryManager(readDB *gorm.DB) QueryManager {
	return queryManager{inner: dbx.NewQueryRunner(readDB, func(db *gorm.DB) QueryWork {
		return &queryWork{db: db}
	})}
}

func (m queryManager) Do(ctx context.Context, fn func(QueryWork) error) error {
	return m.inner.Do(ctx, fn)
}
