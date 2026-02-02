// micro/ledger/internal/infra/out/persistence/gorm/entity/payment.go
package entity

import (
	"time"

	"github.com/google/uuid"
	"github.com/shopspring/decimal"
)

type PaymentEntity struct {
	ID             uuid.UUID       `gorm:"type:uuid;default:gen_random_uuid();primaryKey"`
	IdempotencyKey string          `gorm:"type:varchar(255);uniqueIndex;not null"`
	SourceAccount  uuid.UUID       `gorm:"type:uuid;not null;index"`
	DestAccount    uuid.UUID       `gorm:"type:uuid;not null;index"`
	Amount         decimal.Decimal `gorm:"type:numeric(20,6);not null"`
	Currency       string          `gorm:"type:char(3);not null"`

	// Estado del proceso (posted|failed|reversed|...)
	Status string `gorm:"type:varchar(32);not null;index"`

	// Si tu canal web siempre lo trae, déjalo NOT NULL.
	// Si tienes escenarios internos donde no viene, cámbialo a *uuid.UUID (nullable).
	CustomerID *uuid.UUID `gorm:"type:uuid;not null;index"`

	// --- SAGA / HOLD / LEDGER (claves para compensación y trazabilidad) ---
	// hold_id: debe ser estable. Recomendación: usar payment_id como hold_id.
	HoldID *uuid.UUID `gorm:"type:uuid;not null;index"`

	// journal_id: se llena cuando posteas el asiento.
	JournalID *uuid.UUID `gorm:"type:uuid;index"`

	// opcional: útil para debug cross-service
	CorrelationID string `gorm:"type:varchar(128);index"`

	CreatedAt time.Time `gorm:"type:timestamptz;not null"`
	UpdatedAt time.Time `gorm:"type:timestamptz;not null;index"`

	Steps []PaymentStepEntity `gorm:"foreignKey:PaymentID;constraint:OnDelete:CASCADE"`
}

type PaymentStepEntity struct {
	ID        uuid.UUID `gorm:"type:uuid;default:gen_random_uuid();primaryKey"`
	PaymentID uuid.UUID `gorm:"type:uuid;not null;index"`

	// step: reserve_hold | post_ledger | release_hold | ...
	Step  string `gorm:"type:varchar(64);not null;index"`
	State string `gorm:"type:varchar(32);not null;index"`

	DetailsJSON string    `gorm:"type:jsonb;not null;default:'{}'"`
	AttemptedAt time.Time `gorm:"type:timestamptz;not null"`
}

func (PaymentEntity) TableName() string     { return "payments" }
func (PaymentStepEntity) TableName() string { return "payment_steps" }
