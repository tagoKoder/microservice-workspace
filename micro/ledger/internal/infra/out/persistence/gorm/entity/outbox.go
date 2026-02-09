// micro\ledger\internal\infra\out\persistence\gorm\entity\outbox.go
package entity

import (
	"time"

	"github.com/google/uuid"
)

type OutboxEventEntity struct {
	ID              uuid.UUID  `gorm:"type:uuid;default:gen_random_uuid();primaryKey"`
	AggregateType   string     `gorm:"type:text;not null;index"`
	AggregateID     uuid.UUID  `gorm:"type:uuid;not null;index"`
	EventType       string     `gorm:"type:text;not null;index"`
	PayloadJSON     string     `gorm:"column:payload;type:jsonb;not null"`
	Published       bool       `gorm:"not null;default:false;index"`
	PublishedAt     *time.Time `gorm:"type:timestamptz"`
	Processing      bool       `gorm:"not null;default:false;index"`
	ProcessingOwner *string    `gorm:"type:text"`
	ProcessingAt    *time.Time `gorm:"type:timestamptz"`
	CreatedAt       time.Time  `gorm:"type:timestamptz;not null;index"`
}

func (OutboxEventEntity) TableName() string { return "outbox_events" }
