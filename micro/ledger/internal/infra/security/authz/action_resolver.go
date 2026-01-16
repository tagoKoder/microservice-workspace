package authz

// Action (catálogo) + route_template (obligatorio para AuditEvent v1.0).
type Action struct {
	ID           string // p.ej. "ledger.payments.post"
	RouteTemplate string // p.ej. "POST /ledgerpayments.v1.PaymentsService/PostPayment"
}

// Resuelve action a partir del gRPC fullMethod.
// fullMethod ejemplo: "/bank.ledgerpayments.v1.PaymentsService/PostPayment"
type ActionResolver struct{}

func NewActionResolver() *ActionResolver { return &ActionResolver{} }

func (r *ActionResolver) Resolve(fullMethod string) Action {
	switch fullMethod {
	case "/bank.ledgerpayments.v1.PaymentsService/PostPayment":
		return Action{ID: "ledger.payments.post", RouteTemplate: "POST " + fullMethod}
	case "/bank.ledgerpayments.v1.PaymentsService/GetPayment":
		return Action{ID: "ledger.payments.get", RouteTemplate: "GET " + fullMethod}
	case "/bank.ledgerpayments.v1.LedgerService/CreditAccount":
		return Action{ID: "ledger.accounts.credit", RouteTemplate: "POST " + fullMethod}
	case "/bank.ledgerpayments.v1.LedgerService/ListAccountJournalEntries":
		return Action{ID: "ledger.journals.list", RouteTemplate: "GET " + fullMethod}
	case "/bank.ledgerpayments.v1.LedgerService/CreateManualJournalEntry":
		return Action{ID: "ledger.journals.manual.create", RouteTemplate: "POST " + fullMethod}
	default:
		// Nunca dejes route_template vacío: tu estándar lo exige.
		return Action{ID: "unknown", RouteTemplate: "CALL " + fullMethod}
	}
}
