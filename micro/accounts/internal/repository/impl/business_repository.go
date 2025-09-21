package repository

import (
	"context"
	"errors"
	"time"

	"gorm.io/gorm"
	"gorm.io/gorm/clause"

	"github.com/tagoKoder/accounts/internal/domain/model"
	"github.com/tagoKoder/accounts/internal/repository"
)

// Ensure businessRepo implements the interfaces
var _ repository.BusinessReadRepository = (*businessRepo)(nil)
var _ repository.BusinessWriteRepository = (*businessRepo)(nil)

type businessRepo struct {
	db *gorm.DB // this *db* is the active transaction of the Unit of Work
}

func NewBusinessRepository(db *gorm.DB) repository.BusinessRepository {
	return &businessRepo{db: db}
}

// --- READ ---
func (r *businessRepo) GetByID(ctx context.Context, id int64) (*model.Business, error) {
	var m model.Business
	err := r.db.WithContext(ctx).First(&m, "id = ?", id).Error
	if errors.Is(err, gorm.ErrRecordNotFound) {
		return nil, nil
	}
	return &m, err
}

func (r *businessRepo) GetByGovernmentID(ctx context.Context, govID string) (*model.Business, error) {
	var m model.Business
	err := r.db.WithContext(ctx).
		Where("government_id = ?", govID).
		First(&m).Error
	if errors.Is(err, gorm.ErrRecordNotFound) {
		return nil, nil
	}
	return &m, err
}

// --- WRITE ---
func (r *businessRepo) Create(ctx context.Context, b *model.Business) error {
	now := time.Now().UTC()
	if b.CreatedAt.IsZero() {
		b.CreatedAt = now
	}
	b.UpdatedAt = now
	return r.db.WithContext(ctx).Create(b).Error
}

func (r *businessRepo) UpdateCore(ctx context.Context, b *model.Business) error {
	// Asumimos que tienes el objeto con los campos actualizados (Name, TimeZoneID)
	// y que no quieres tocar government_id desde aquí.
	b.UpdatedAt = time.Now().UTC()
	// Usa Updates para enviar solo los campos no-cero que cambias (si quieres control total, usa map[string]any)
	res := r.db.WithContext(ctx).
		Model(&model.Business{}).
		Where("id = ?", b.ID).
		Updates(map[string]any{
			"name":       b.Name,
			"updated_at": b.UpdatedAt,
		})
	if res.Error != nil {
		return res.Error
	}
	if res.RowsAffected == 0 {
		return gorm.ErrRecordNotFound
	}
	return nil
}

func (r *businessRepo) SoftDelete(ctx context.Context, id int64) error {
	res := r.db.WithContext(ctx).
		Where("id = ?", id).
		Delete(&model.Business{})
	if res.Error != nil {
		return res.Error
	}
	if res.RowsAffected == 0 {
		return gorm.ErrRecordNotFound
	}
	return nil
}

func (r *businessRepo) Restore(ctx context.Context, id int64) error {
	// Unscoped() para poder tocar deleted_at
	return r.db.WithContext(ctx).
		Model(&model.Business{}).
		Unscoped().
		Where("id = ?", id).
		Update("deleted_at", clause.Expr{SQL: "NULL"}).Error
}
