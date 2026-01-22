// micro\ledger\internal\infra\out\persistence\gorm\adapter\idempotency_repo.go
package adapter

import (
	"context"
	"errors"

	"github.com/tagoKoder/ledger/internal/domain/model"
	out "github.com/tagoKoder/ledger/internal/domain/port/out"
	"github.com/tagoKoder/ledger/internal/infra/out/persistence/gorm/entity"
	"gorm.io/gorm"
	"gorm.io/gorm/clause"
)

type IdempotencyRepo struct{ db *gorm.DB }

func NewIdempotencyRepo(db *gorm.DB) out.IdempotencyRepositoryPort { return &IdempotencyRepo{db: db} }

func (r *IdempotencyRepo) Get(ctx context.Context, key string) (*model.IdempotencyRecord, error) {
	var rec entity.IdempotencyRecordEntity
	err := r.db.WithContext(ctx).First(&rec, "key = ?", key).Error
	if errors.Is(err, gorm.ErrRecordNotFound) {
		return &model.IdempotencyRecord{}, nil
	}
	if err != nil {
		return nil, err
	}
	return &model.IdempotencyRecord{
		Key:          rec.Key,
		Operation:    rec.Operation,
		ResponseJSON: rec.ResponseJSON,
		StatusCode:   rec.StatusCode,
		CreatedAt:    rec.CreatedAt,
	}, nil
}

func (r *IdempotencyRepo) Put(ctx context.Context, rec *model.IdempotencyRecord) error {
	m := entity.IdempotencyRecordEntity{
		Key:          rec.Key,
		Operation:    rec.Operation,
		ResponseJSON: rec.ResponseJSON,
		StatusCode:   rec.StatusCode,
		CreatedAt:    rec.CreatedAt,
	}
	return r.db.WithContext(ctx).
		Clauses(clause.OnConflict{Columns: []clause.Column{{Name: "key"}}, DoNothing: true}).
		Create(&m).Error
}
