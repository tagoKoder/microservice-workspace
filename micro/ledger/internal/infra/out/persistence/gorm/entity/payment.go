package entity

import (
	"time"

	"github.com/google/uuid"
	"github.com/shopspring/decimal"
)

type PaymentEntity struct {
	ID             uuid.UUID           `gorm:"type:uuid;default:gen_random_uuid();primaryKey"`
	IdempotencyKey string              `gorm:"type:varchar(255);uniqueIndex;not null"`
	SourceAccount  uuid.UUID           `gorm:"type:uuid;not null;index"`
	DestAccount    uuid.UUID           `gorm:"type:uuid;not null;index"`
	Amount         decimal.Decimal     `gorm:"type:numeric(20,6);not null"`
	Currency       string              `gorm:"type:char(3);not null"`
	Status         string              `gorm:"type:text;not null"`
	CreatedAt      time.Time           `gorm:"type:timestamptz;not null"`
	Steps          []PaymentStepEntity `gorm:"foreignKey:PaymentID;constraint:OnDelete:CASCADE"`
}

type PaymentStepEntity struct {
	ID          uuid.UUID `gorm:"type:uuid;default:gen_random_uuid();primaryKey"`
	PaymentID   uuid.UUID `gorm:"type:uuid;not null;index"`
	Step        string    `gorm:"type:text;not null"`
	State       string    `gorm:"type:text;not null"`
	DetailsJSON string    `gorm:"type:jsonb;not null;default:'{}'"`
	AttemptedAt time.Time `gorm:"type:timestamptz;not null"`
}

func (PaymentEntity) TableName() string     { return "payments" }
func (PaymentStepEntity) TableName() string { return "payment_steps" }
