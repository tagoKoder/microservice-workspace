package entity

import (
	"time"

	"github.com/google/uuid"
	"github.com/shopspring/decimal"
)

type JournalEntryEntity struct {
	ID          uuid.UUID         `gorm:"type:uuid;default:gen_random_uuid();primaryKey"`
	ExternalRef string            `gorm:"type:text"`
	BookedAt    time.Time         `gorm:"type:timestamptz;not null"`
	CreatedBy   string            `gorm:"type:text;not null"`
	Status      string            `gorm:"type:text;not null"`
	Currency    string            `gorm:"type:char(3);not null"`
	Lines       []EntryLineEntity `gorm:"foreignKey:JournalID;constraint:OnDelete:CASCADE"`
}

type EntryLineEntity struct {
	ID              uuid.UUID       `gorm:"type:uuid;default:gen_random_uuid();primaryKey"`
	JournalID       uuid.UUID       `gorm:"type:uuid;not null;index"`
	GLAccountID     uuid.UUID       `gorm:"type:uuid;not null;index"`
	GLAccountCode   string          `gorm:"type:varchar(20);not null"`
	CounterpartyRef *string         `gorm:"type:text"`
	Debit           decimal.Decimal `gorm:"type:numeric(20,6);not null;default:0"`
	Credit          decimal.Decimal `gorm:"type:numeric(20,6);not null;default:0"`
}

func (JournalEntryEntity) TableName() string { return "journal_entries" }
func (EntryLineEntity) TableName() string    { return "entry_lines" }
