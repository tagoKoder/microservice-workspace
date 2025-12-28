package adapter

import (
	"context"
	"errors"

	"github.com/tagoKoder/ledger/internal/domain/model"
	out "github.com/tagoKoder/ledger/internal/domain/port/out"
	"github.com/tagoKoder/ledger/internal/infra/out/persistence/gorm/entity"
	"gorm.io/gorm"
)

type IdempotencyRepo struct{ db *gorm.DB }

func NewIdempotencyRepo(db *gorm.DB) out.IdempotencyRepositoryPort { return &IdempotencyRepo{db: db} }

func (r *IdempotencyRepo) Get(ctx context.Context, key string) (*model.IdempotencyRecord, error) {
	var rec entity.IdempotencyRecordEntity
	err := r.db.WithContext(ctx).First(&rec, "key = ?", key).Error
	if errors.Is(err, gorm.ErrRecordNotFound) {
		return nil, nil
	}
	return &model.IdempotencyRecord{
		Key:          rec.Key,
		ResponseJSON: rec.ResponseJSON,
		CreatedAt:    rec.CreatedAt,
	}, err
}

func (r *IdempotencyRepo) Put(ctx context.Context, rec *model.IdempotencyRecord) error {
	return r.db.WithContext(ctx).Create(&entity.IdempotencyRecordEntity{
		Key:          rec.Key,
		ResponseJSON: rec.ResponseJSON,
		CreatedAt:    rec.CreatedAt,
	}).Error
}
