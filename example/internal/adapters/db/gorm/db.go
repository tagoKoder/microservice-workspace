package gormdb

import (
	"fmt"
	"log"
	"time"

	"github.com/tagoKoder/clinic/internal/domain"
	"gorm.io/driver/postgres"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

// Modelos persistentes (mapeo GORM con esquema "order")
type orderModel struct {
	ID        string `gorm:"primaryKey;type:uuid"`
	UserID    string `gorm:"type:uuid;not null;index"`
	Status    string `gorm:"type:varchar(16);not null;index"`
	CreatedAt time.Time
	UpdatedAt time.Time
}

func (orderModel) TableName() string { return `"order".orders` }

type orderItemModel struct {
	ID        string `gorm:"primaryKey;type:uuid"`
	OrderID   string `gorm:"type:uuid;not null;index"`
	SKU       string `gorm:"type:varchar(64);not null"`
	Qty       int    `gorm:"not null"`
	UnitPrice int64  `gorm:"not null"`
	CreatedAt time.Time
	UpdatedAt time.Time
}

func (orderItemModel) TableName() string { return `"order".order_items` }

func NewDB(dsn string) (*gorm.DB, error) {
	cfg := &gorm.Config{
		Logger:      logger.Default.LogMode(logger.Warn),
		PrepareStmt: true,
	}
	db, err := gorm.Open(postgres.Open(dsn), cfg)
	if err != nil {
		return nil, err
	}

	sqlDB, err := db.DB()
	if err != nil {
		return nil, err
	}
	sqlDB.SetMaxIdleConns(10)
	sqlDB.SetMaxOpenConns(50)
	sqlDB.SetConnMaxLifetime(30 * time.Minute)

	// Crear esquema y migrar
	if err := db.Exec(`CREATE SCHEMA IF NOT EXISTS "order";`).Error; err != nil {
		return nil, fmt.Errorf("create schema: %w", err)
	}
	if err := db.AutoMigrate(&orderModel{}, &orderItemModel{}); err != nil {
		return nil, fmt.Errorf("migrate: %w", err)
	}
	log.Println("DB ready (schema: order)")
	return db, nil
}

// Mapeos de dominio <-> persistencia

func toOrderModel(o *domain.Order) *orderModel {
	return &orderModel{
		ID:        o.ID,
		UserID:    o.UserID,
		Status:    string(o.Status),
		CreatedAt: o.CreatedAt,
		UpdatedAt: o.UpdatedAt,
	}
}

func toOrderItemModels(items []domain.OrderItem, orderID string) []orderItemModel {
	out := make([]orderItemModel, 0, len(items))
	for _, it := range items {
		out = append(out, orderItemModel{
			ID:        it.ID,
			OrderID:   orderID,
			SKU:       it.SKU,
			Qty:       it.Qty,
			UnitPrice: it.UnitPrice,
			CreatedAt: it.CreatedAt,
			UpdatedAt: it.UpdatedAt,
		})
	}
	return out
}

func toDomainOrder(m orderModel, itemModels []orderItemModel) *domain.Order {
	o := &domain.Order{
		ID:        m.ID,
		UserID:    m.UserID,
		Status:    domain.OrderStatus(m.Status),
		CreatedAt: m.CreatedAt,
		UpdatedAt: m.UpdatedAt,
	}
	items := make([]domain.OrderItem, 0, len(itemModels))
	for _, im := range itemModels {
		items = append(items, domain.OrderItem{
			ID:        im.ID,
			OrderID:   im.OrderID,
			SKU:       im.SKU,
			Qty:       im.Qty,
			UnitPrice: im.UnitPrice,
			CreatedAt: im.CreatedAt,
			UpdatedAt: im.UpdatedAt,
		})
	}
	o.Items = items
	return o
}
