package model

import "time"

type TimeZone struct {
	ID        int64     `gorm:"primaryKey;autoIncrement" json:"id"`
	Name      string    `gorm:"type:varchar(64);not null;uniqueIndex" json:"name"`
	CreatedAt time.Time `gorm:"not null" json:"created_at"` // timestamptz
}
