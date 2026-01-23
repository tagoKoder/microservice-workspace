package ports

type Clients struct {
	Identity       IdentityPort
	Accounts       AccountsPort
	LedgerPayments LedgerPaymentsPort
	Close          func() error
}
