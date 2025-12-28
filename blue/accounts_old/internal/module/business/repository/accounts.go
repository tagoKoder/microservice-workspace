package repository

import (
	"context"

	"github.com/tagoKoder/accounts/internal/module/business/data/model"
)

type AccountsRepository interface {
	GetCustomerAccountByIDAndType(ctx context.Context, id *int64, accountType string) (*model.Accounts, error)
	GetAccountBalancesByAccountID(ctx context.Context, accountID *int64) (*model.AccountBalances, error)
	CreateCustomerAccount(ctx context.Context, a *model.Accounts) error
	CreateAccountBalances(ctx context.Context, ab *model.AccountBalances) error
}
