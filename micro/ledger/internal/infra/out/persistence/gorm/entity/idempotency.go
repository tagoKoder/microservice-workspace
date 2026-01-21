// micro\ledger\internal\infra\out\persistence\gorm\entity\idempotency.go
package entity

import (
	"time"

	"github.com/google/uuid"
)

type IdempotencyRecordEntity struct {
	ID           uuid.UUID `gorm:"type:uuid;default:gen_random_uuid();primaryKey"`
	Key          string    `gorm:"type:varchar(255);uniqueIndex;not null"`
	Operation    string    `gorm:"type:text;not null"`
	ResponseJSON string    `gorm:"type:jsonb;not null"`
	StatusCode   int       `gorm:"not null"`
	CreatedAt    time.Time `gorm:"type:timestamptz;not null;index"`
}

func (IdempotencyRecordEntity) TableName() string { return "idempotency_records" }
