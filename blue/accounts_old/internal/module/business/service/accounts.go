package service

import (
	"context"

	accountspb "github.com/tagoKoder/accounts/proto/genproto/go/accounts"
)

type AccountsService interface {
	CreateCustomerAccount(ctx context.Context, in *accountspb.CreateCustomerAccountRequest) (*accountspb.CreateCustomerAccountResponse, error)
}
