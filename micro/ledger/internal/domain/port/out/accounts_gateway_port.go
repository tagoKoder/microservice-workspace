package out

import (
	"context"

	"github.com/google/uuid"
	"github.com/shopspring/decimal"
)

type AccountsGatewayPort interface {
	ValidateAccountsAndLimits(ctx context.Context, source, dest uuid.UUID, currency string, amount decimal.Decimal) error
	ReserveHold(ctx context.Context, source uuid.UUID, currency string, amount decimal.Decimal) error
	ReleaseHold(ctx context.Context, source uuid.UUID, currency string, amount decimal.Decimal) error
}
