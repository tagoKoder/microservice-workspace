package authz

import (
	"context"
	"strings"

	vptypes "github.com/aws/aws-sdk-go-v2/service/verifiedpermissions/types"
	"github.com/google/uuid"
	"github.com/tagoKoder/ledger/internal/application/uow"
)

type Resolved struct {
	ResourceType    string
	ResourceID      string
	ContextAttrs    map[string]vptypes.AttributeValue
	OwnerCustomerID string
}

type Templates struct {
	uow uow.UnitOfWorkManager
}

func NewTemplates(u uow.UnitOfWorkManager) *Templates { return &Templates{uow: u} }

func (t *Templates) Resolve(ctx context.Context, template string, req any) (Resolved, error) {
	switch template {
	case T_PAYMENT_NEW:
		return Resolved{ResourceType: "Payment", ResourceID: "new"}, nil
	case T_PAYMENT_BY_ID:
		id := ExtractStringField(req, "PaymentId")
		if id == "" {
			id = ExtractStringField(req, "payment_id")
		}
		return Resolved{ResourceType: "Payment", ResourceID: id, OwnerCustomerID: t.lookupPaymentOwner(ctx, id)}, nil

	case T_ACCOUNT_BY_ID, T_ACCOUNT_STATEMENT:
		acc := ExtractStringField(req, "AccountId")
		if acc == "" {
			acc = ExtractStringField(req, "account_id")
		}
		return Resolved{ResourceType: "Account", ResourceID: acc}, nil

	case T_ACCOUNT_SYSTEM_CREDIT:
		// Strong context: reason + external_ref + amount
		acc := ExtractStringField(req, "AccountId")
		if acc == "" {
			acc = ExtractStringField(req, "account_id")
		}

		ext := ExtractStringField(req, "ExternalRef")
		if ext == "" {
			ext = ExtractStringField(req, "external_ref")
		}

		reason := ExtractStringField(req, "Reason")
		if reason == "" {
			reason = ExtractStringField(req, "reason")
		}

		amt := ExtractStringField(req, "Amount")
		if amt == "" {
			amt = ExtractStringField(req, "amount")
		}

		ctxm := map[string]vptypes.AttributeValue{
			"external_ref": &vptypes.AttributeValueMemberString{Value: ext},
			"reason":       &vptypes.AttributeValueMemberString{Value: strings.ToLower(reason)},
			"amount":       &vptypes.AttributeValueMemberString{Value: amt},
		}

		return Resolved{ResourceType: "Account", ResourceID: acc, ContextAttrs: ctxm}, nil

	default:
		return Resolved{ResourceType: "Unknown", ResourceID: "unknown"}, nil
	}
}

func (t *Templates) lookupPaymentOwner(ctx context.Context, paymentID string) string {
	pid, err := uuid.Parse(paymentID)
	if err != nil {
		return ""
	}
	var owner string
	_ = t.uow.DoRead(ctx, func(rr uow.ReadRepos) error {
		p, err := rr.Payments().FindById(ctx, pid)
		if err != nil || p == nil {
			return nil
		}
		owner = extractUUIDStringField(p, "CustomerID")
		return nil
	})
	return owner
}
