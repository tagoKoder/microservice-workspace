package gormdb

import (
	"context"
	"time"

	"github.com/google/uuid"
	"github.com/tagoKoder/clinic/internal/domain"
	"github.com/tagoKoder/clinic/internal/ports"
	"gorm.io/gorm"
)

type orderRepo struct{ db *gorm.DB }

func NewOrderRepository(db *gorm.DB) ports.OrderRepository {
	return &orderRepo{db: db}
}

func (r *orderRepo) Save(ctx context.Context, o *domain.Order) error {
	now := time.Now().UTC()
	if o.ID == "" {
		o.ID = uuid.NewString()
		o.CreatedAt = now
	}
	o.UpdatedAt = now
	for i := range o.Items {
		if o.Items[i].ID == "" {
			o.Items[i].ID = uuid.NewString()
			o.Items[i].CreatedAt = now
		}
		o.Items[i].UpdatedAt = now
		o.Items[i].OrderID = o.ID
	}

	// Upsert de Order + Items (simple: delete+insert items para mantenerlo claro)
	if err := r.db.WithContext(ctx).Clauses().Save(toOrderModel(o)).Error; err != nil {
		return err
	}
	// reemplazamos items (una estrategia simple para este ejemplo)
	if err := r.db.WithContext(ctx).
		Where(`"order_id" = ?`, o.ID).
		Delete(&orderItemModel{}).Error; err != nil {
		return err
	}
	if len(o.Items) > 0 {
		if err := r.db.WithContext(ctx).Create(toOrderItemModels(o.Items, o.ID)).Error; err != nil {
			return err
		}
	}
	return nil
}

func (r *orderRepo) GetByID(ctx context.Context, id string) (*domain.Order, error) {
	var om orderModel
	if err := r.db.WithContext(ctx).First(&om, `"id" = ?`, id).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			return nil, nil
		}
		return nil, err
	}
	var ims []orderItemModel
	if err := r.db.WithContext(ctx).Where(`"order_id" = ?`, id).Find(&ims).Error; err != nil {
		return nil, err
	}
	return toDomainOrder(om, ims), nil
}
