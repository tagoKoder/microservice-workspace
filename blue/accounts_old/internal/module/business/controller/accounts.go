package controller

import (
	"context"

	"github.com/tagoKoder/accounts/internal/module/business/service"
	accountspb "github.com/tagoKoder/accounts/proto/genproto/go/accounts"
)

type AccountsController struct {
	service service.AccountsService
}

func NewAccountsController(service service.AccountsService) *AccountsController {
	return &AccountsController{service: service}
}

func (c *AccountsController) CreateCustomerAccount(ctx context.Context, req *accountspb.CreateCustomerAccountRequest) (*accountspb.CreateCustomerAccountResponse, error) {

	return c.service.CreateCustomerAccount(ctx, req)
}
