package model

import (
	"time"

	"gorm.io/gorm"
)

type Business struct {
	ID           int64  `gorm:"primaryKey;autoIncrement" json:"id"`
	TimeZoneID   int64  `gorm:"not null;index"           json:"time_zone_id"`
	Name         string `gorm:"type:varchar(64);not null" json:"name"`
	GovernmentID string `gorm:"type:varchar(16);not null;index" json:"government_id"`

	CreatedAt time.Time      `gorm:"not null" json:"created_at"` // timestamptz
	UpdatedAt time.Time      `gorm:"not null" json:"updated_at"` // timestamptz
	DeletedAt gorm.DeletedAt `gorm:"index"    json:"deleted_at"` // soft delete

	// Relación (opcional): si quieres precargar la zona
	TimeZone *TimeZone `gorm:"foreignKey:TimeZoneID;references:ID" json:"time_zone,omitempty"`
}
