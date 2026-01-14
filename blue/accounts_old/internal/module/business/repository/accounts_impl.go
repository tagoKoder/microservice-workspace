package repository

import (
	"context"
	"time"

	"github.com/tagoKoder/accounts/internal/module/business/data/model"
	"gorm.io/gorm"
)

type accountsRepository struct {
	db *gorm.DB
}

func NewAccountsRepository(db *gorm.DB) AccountsRepository {
	return &accountsRepository{db: db}
}

// READ
func (r *accountsRepository) GetCustomerAccountByIDAndType(ctx context.Context, id *int64, accountType string) (*model.Accounts, error) {
	var account model.Accounts
	err := r.db.WithContext(ctx).First(&account, "id = ? AND type = ?", id, accountType).Error
	if err != nil {
		return nil, err
	}
	return &account, nil
}

func (r *accountsRepository) GetAccountBalancesByAccountID(ctx context.Context, accountID *int64) (*model.AccountBalances, error) {
	var balances model.AccountBalances
	err := r.db.WithContext(ctx).First(&balances, "account_id = ?", accountID).Error
	if err != nil {
		return nil, err
	}
	return &balances, nil
}

// WRITE

func (r *accountsRepository) CreateCustomerAccount(ctx context.Context, a *model.Accounts) error {
	now := time.Now().UTC()
	if a.OpenAt.IsZero() {
		a.OpenAt = now
	}
	if err := r.db.WithContext(ctx).Create(a).Error; err != nil {
		return err
	}
	return nil
}

func (r *accountsRepository) CreateAccountBalances(ctx context.Context, ab *model.AccountBalances) error {
	if err := r.db.WithContext(ctx).Create(ab).Error; err != nil {
		return err
	}
	return nil
}
