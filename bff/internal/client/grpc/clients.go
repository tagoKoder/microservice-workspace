// bff\internal\client\grpc\clients.go
package grpc

import (
	"fmt"
	"log"

	"github.com/tagoKoder/bff/internal/client/ports"
	"github.com/tagoKoder/bff/internal/config"
)

func NewClients(cfg config.Config) (*ports.Clients, error) {
	idConn, err := dial(cfg.IdentityGRPCAddr, cfg)
	if err != nil {
		log.Printf("identity address: %s", cfg.IdentityGRPCAddr)
		return nil, fmt.Errorf("identity dial: %w", err)
	}
	accConn, err := dial(cfg.AccountsGRPCAddr, cfg)
	if err != nil {
		_ = idConn.Close()
		log.Printf("accounts address: %s", cfg.AccountsGRPCAddr)
		return nil, fmt.Errorf("accounts dial: %w", err)
	}
	lpConn, err := dial(cfg.LedgerPaymentsGRPCAddr, cfg)
	if err != nil {
		_ = idConn.Close()
		_ = accConn.Close()
		log.Printf("ledger payments address: %s", cfg.LedgerPaymentsGRPCAddr)
		return nil, fmt.Errorf("ledgerpayments dial: %w", err)
	}

	return &ports.Clients{
		Identity:       NewIdentityClient(idConn),
		Accounts:       NewAccountsClient(accConn),
		LedgerPayments: NewLedgerPaymentsClient(lpConn),
		Close: func() error {
			_ = idConn.Close()
			_ = accConn.Close()
			_ = lpConn.Close()
			return nil
		},
	}, nil
}
