package service

import (
	"errors"

	"github.com/shopspring/decimal"
	"github.com/tagoKoder/ledger/internal/domain/model"
)

func EnsureBalanced(lines []model.EntryLine) error {
	var d, c decimal.Decimal
	for _, l := range lines {
		d = d.Add(l.Debit)
		c = c.Add(l.Credit)
	}
	if !d.Equal(c) {
		return errors.New("journal not balanced (debit != credit)")
	}
	return nil
}
