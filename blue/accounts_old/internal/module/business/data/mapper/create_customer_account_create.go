package mapper

import (
	"github.com/tagoKoder/accounts/internal/module/business/data/model"
	accountspb "github.com/tagoKoder/accounts/proto/genproto/go/accounts"
	"google.golang.org/protobuf/types/known/timestamppb"
)

func ToAccountsModel(pb *accountspb.CreateCustomerAccountRequest) *model.Accounts {
	return &model.Accounts{
		CustomerID: pb.GetCustomerId(),
		Type:       pb.GetAccountType(),
		Currency:   pb.GetCurrency(),
		Status:     "active",
		InitialAmt: pb.GetInitialDeposit(),
	}
}

func ToAccountBalancesModel(accountID int64, initialAmt float64) *model.AccountBalances {
	return &model.AccountBalances{
		AccountID: accountID,
		Available: initialAmt,
		Ledger:    initialAmt,
		Hold:      0,
	}
}

func ToCustomerAccountResPb(a *model.Accounts, ab *model.AccountBalances) *accountspb.CreateCustomerAccountResponse {
	return &accountspb.CreateCustomerAccountResponse{
		Account: &accountspb.Account{
			AccountId:   a.ID,
			CustomerId:  a.CustomerID,
			AccountType: a.Type,
			Currency:    a.Currency,
			Status:      a.Status,
			Balance:     ab.Available,
			OpenAt:      timestamppb.New(a.OpenAt),
		},
	}

}
