package model

import "time"

type Accounts struct {
	ID         int64     `gorm:"primaryKey;autoIncrement" json:"id"`
	CustomerID int64     `gorm:"type:bigint;not null" json:"customer_id"`
	Type       string    `gorm:"type:varchar(32);not null" json:"type"`    // e.g., "savings", "checking"
	Currency   string    `gorm:"type:varchar(3);not null" json:"currency"` // e.g., "USD", "EUR"
	InitialAmt float64   `gorm:"type:numeric(20,6);not null;default:0" json:"initial_amt"`
	Status     string    `gorm:"type:varchar(16);not null" json:"status"` // e.g., "active", "inactive", "closed"
	OpenAt     time.Time `gorm:"not null" json:"open_at"`                 // timestamp
}
