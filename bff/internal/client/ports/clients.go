package ports

type Clients struct {
	Identity       IdentityPort
	Accounts       AccountsPort
	LedgerPayments LedgerPaymentsPort
	Ops            OpsPort
	Close          func() error
}
