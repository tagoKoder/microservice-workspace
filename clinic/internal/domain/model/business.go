package model

import (
	"time"

	"gorm.io/gorm"
)

type Business struct {
	ID           int64  `gorm:"primaryKey;autoIncrement" json:"id"`
	Name         string `gorm:"type:varchar(64);not null" json:"name"`
	GovernmentID string `gorm:"type:varchar(16);not null;index" json:"government_id"`

	CreatedAt time.Time      `gorm:"not null" json:"created_at"` // timestamptz
	UpdatedAt time.Time      `gorm:"not null" json:"updated_at"` // timestamptz
	DeletedAt gorm.DeletedAt `gorm:"index"    json:"deleted_at"` // soft delete
}
