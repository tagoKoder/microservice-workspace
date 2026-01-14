package model

type AccountBalances struct {
	AccountID int64   `gorm:"primaryKey;autoIncrement:false" json:"account_id"`
	Ledger    float64 `gorm:"type:numeric(20,6);not null;default:0" json:"ledger"`    // saldo contable
	Available float64 `gorm:"type:numeric(20,6);not null;default:0" json:"available"` // saldo disponible
	Hold      float64 `gorm:"type:numeric(20,6);not null;default:0" json:"hold"`      // saldo en espera
}
