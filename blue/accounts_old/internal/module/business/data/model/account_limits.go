package model

import "time"

type AccountLimits struct {
	AccountID       int64     `gorm:"primaryKey;autoIncrement:false" json:"account_id"`
	DailyWithdraw   float64   `gorm:"type:numeric(20,6);not null;default:0" json:"daily_withdraw"`   // límite diario de retiro
	DailyDeposit    float64   `gorm:"type:numeric(20,6);not null;default:0" json:"daily_deposit"`    // límite diario de depósito
	MonthlyTransfer float64   `gorm:"type:numeric(20,6);not null;default:0" json:"monthly_transfer"` // límite mensual de transferencia
	UpdatedAt       time.Time `gorm:"not null" json:"updated_at"`                                    // timestamp
}
