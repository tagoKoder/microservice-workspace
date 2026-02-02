package out

import (
	"context"

	accountsv1 "github.com/tagoKoder/ledger/internal/genproto/bank/accounts/v1"
)

type AccountsGatewayPort interface {
	ValidateAccountsAndLimits(ctx context.Context, req *accountsv1.ValidateAccountsAndLimitsRequest) (*accountsv1.ValidateAccountsAndLimitsResponse, error)
	ReserveHold(ctx context.Context, req *accountsv1.ReserveHoldRequest) (*accountsv1.ReserveHoldResponse, error)
	ReleaseHold(ctx context.Context, req *accountsv1.ReleaseHoldRequest) (*accountsv1.ReleaseHoldResponse, error)
}
