package grpc

import (
	"fmt"

	"github.com/tagoKoder/bff/internal/client/ports"
	"github.com/tagoKoder/bff/internal/config"
)

func NewClients(cfg config.Config) (*ports.Clients, error) {
	idConn, err := dial(cfg.IdentityGRPCAddr, cfg)
	if err != nil {
		return nil, fmt.Errorf("identity dial: %w", err)
	}
	accConn, err := dial(cfg.AccountsGRPCAddr, cfg)
	if err != nil {
		_ = idConn.Close()
		return nil, fmt.Errorf("accounts dial: %w", err)
	}
	lpConn, err := dial(cfg.LedgerPaymentsGRPCAddr, cfg)
	if err != nil {
		_ = idConn.Close()
		_ = accConn.Close()
		return nil, fmt.Errorf("ledgerpayments dial: %w", err)
	}
	opsConn, err := dial(cfg.OpsGRPCAddr, cfg)
	if err != nil {
		_ = idConn.Close()
		_ = accConn.Close()
		_ = lpConn.Close()
		return nil, fmt.Errorf("ops dial: %w", err)
	}

	return &ports.Clients{
		Identity:       NewIdentityClient(idConn),
		Accounts:       NewAccountsClient(accConn),
		LedgerPayments: NewLedgerPaymentsClient(lpConn),
		Close: func() error {
			_ = idConn.Close()
			_ = accConn.Close()
			_ = lpConn.Close()
			_ = opsConn.Close()
			return nil
		},
	}, nil
}
