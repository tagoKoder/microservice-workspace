package service

import (
	"context"

	"github.com/tagoKoder/accounts/internal/module/business/data/mapper"
	"github.com/tagoKoder/accounts/internal/uow"
	accountspb "github.com/tagoKoder/accounts/proto/genproto/go/accounts"
)

type accountsService struct {
	tx    uow.TxManager
	query uow.QueryManager
}

func NewAccountsService(tx uow.TxManager, query uow.QueryManager) AccountsService {
	return &accountsService{tx: tx, query: query}
}

func (s *accountsService) CreateCustomerAccount(ctx context.Context, in *accountspb.CreateCustomerAccountRequest) (*accountspb.CreateCustomerAccountResponse, error) {
	a := mapper.ToAccountsModel(in)
	var respb *accountspb.CreateCustomerAccountResponse
	err := s.tx.Do(ctx, func(uow uow.UnitOfWork) error {
		if err := uow.AccountsRep().CreateCustomerAccount(ctx, a); err != nil {
			return err
		}
		ab := mapper.ToAccountBalancesModel(a.ID, in.GetInitialDeposit())
		if err := uow.AccountsRep().CreateAccountBalances(ctx, ab); err != nil {
			return err
		}
		respb = mapper.ToCustomerAccountResPb(a, ab)
		return nil
	})
	if err != nil {
		return nil, err
	}
	return respb, nil
}
