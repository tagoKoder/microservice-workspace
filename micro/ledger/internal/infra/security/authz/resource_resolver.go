package authz

import (
	"context"

	"github.com/google/uuid"
	"github.com/tagoKoder/ledger/internal/application/uow"
)

// Resource para AVP.
type Resource struct {
	Type            string // p.ej. "Payment"
	ID              string // uuid string o "new"
	OwnerCustomerID string // opcional (si lo puedes resolver)
}

// ResourceResolver intenta extraer resource_id del request (cuando aplica)
// y opcionalmente lookup owner (por ejemplo, en reads de Payment).
type ResourceResolver struct {
	uow uow.UnitOfWorkManager
}

func NewResourceResolver(u uow.UnitOfWorkManager) *ResourceResolver {
	return &ResourceResolver{uow: u}
}

func (r *ResourceResolver) Resolve(ctx context.Context, fullMethod string, req any) Resource {
	switch fullMethod {
	case "/bank.ledgerpayments.v1.PaymentsService/GetPayment":
		// req: *ledgerpb.GetPaymentRequest (pero evitamos acoplar a pb aquí).
		id := ExtractStringField(req, "PaymentId")
		if id == "" {
			id = ExtractStringField(req, "payment_id")
		}
		res := Resource{Type: "Payment", ID: id}

		// Owner lookup (cierra el círculo si agregas payments.customer_id).
		owner := r.lookupPaymentOwner(ctx, id)
		if owner != "" {
			res.OwnerCustomerID = owner
		}
		return res

	case "/bank.ledgerpayments.v1.PaymentsService/PostPayment":
		return Resource{Type: "Payment", ID: "new"}

	case "/bank.ledgerpayments.v1.LedgerService/CreditAccount":
		acc := ExtractStringField(req, "AccountId")
		if acc == "" {
			acc = ExtractStringField(req, "account_id")
		}
		return Resource{Type: "Account", ID: acc}

	case "/bank.ledgerpayments.v1.LedgerService/ListAccountJournalEntries":
		acc := ExtractStringField(req, "AccountId")
		if acc == "" {
			acc = ExtractStringField(req, "account_id")
		}
		return Resource{Type: "Account", ID: acc}

	case "/bank.ledgerpayments.v1.LedgerService/CreateManualJournalEntry":
		return Resource{Type: "JournalEntry", ID: "new"}

	default:
		return Resource{Type: "Unknown", ID: "unknown"}
	}
}

func (r *ResourceResolver) lookupPaymentOwner(ctx context.Context, paymentID string) string {
	pid, err := uuid.Parse(paymentID)
	if err != nil {
		return ""
	}

	var owner string
	_ = r.uow.DoRead(ctx, func(rr uow.ReadRepos) error {
		p, err := rr.Payments().FindById(ctx, pid)
		if err != nil {
			return nil
		}
		// Requiere que Payment tenga CustomerID (ver diff opcional).
		// Si no existe, queda vacío.
		type customerIDCarrier interface{ GetCustomerID() string }
		_ = customerIDCarrier(nil)

		// Evitamos acoplar por interface; usamos reflection-like por campo si existe:
		owner = extractUUIDStringField(p, "CustomerID")
		return nil
	})
	return owner
}
