// micro\ledger\internal\application\service\payment_service.go
package service

import (
	"context"
	"encoding/json"
	"time"

	"github.com/google/uuid"
	"github.com/shopspring/decimal"
	"github.com/tagoKoder/ledger/internal/application/uow"
	"github.com/tagoKoder/ledger/internal/domain/model"
	dm "github.com/tagoKoder/ledger/internal/domain/model"
	in "github.com/tagoKoder/ledger/internal/domain/port/in"
	out "github.com/tagoKoder/ledger/internal/domain/port/out"
	authctx "github.com/tagoKoder/ledger/internal/infra/security/context"
)

type paymentService struct {
	uow      uow.UnitOfWorkManager
	accounts out.AccountsGatewayPort
	audit    out.AuditPort
}

func NewPaymentService(u uow.UnitOfWorkManager, accounts out.AccountsGatewayPort, audit out.AuditPort) in.PaymentService {
	return &paymentService{uow: u, accounts: accounts, audit: audit}
}

func (s *paymentService) PostPayment(ctx context.Context, cmd in.PostPaymentCommand) (in.PostPaymentResult, error) {
	actor := authctx.ActorFrom(ctx)
	initiatedBy := actor.Subject
	if initiatedBy == "" {
		// fallback temporal (compat)
		initiatedBy = cmd.InitiatedBy
	}

	amt, err := decimal.NewFromString(cmd.Amount)
	if err != nil {
		return in.PostPaymentResult{}, err
	}

	// 1) Idempotencia (read)
	var cachedJSON string
	if err := s.uow.DoRead(ctx, func(r uow.ReadRepos) error {
		rec, err := r.Idempotency().Get(ctx, cmd.IdempotencyKey)
		if err != nil {
			return err
		}
		if rec != nil {
			cachedJSON = rec.ResponseJSON
		}
		return nil
	}); err != nil {
		return in.PostPaymentResult{}, err
	}

	if cachedJSON != "" {
		var pr in.PostPaymentResult
		_ = json.Unmarshal([]byte(cachedJSON), &pr)
		return pr, nil
	}

	// 2) Validación en Accounts (breaker  timeout ya en gateway)
	if err := s.accounts.ValidateAccountsAndLimits(ctx, cmd.SourceAccountID, cmd.DestinationAccount, cmd.Currency, amt); err != nil {
		return in.PostPaymentResult{}, err
	}

	paymentID := uuid.New()
	now := time.Now().UTC()

	// 3) TX: reserve hold  ledger  payment  outbox  idempotency
	var result in.PostPaymentResult
	err = s.uow.DoWrite(ctx, func(w uow.WriteRepos) error {
		// 3.1 reserve hold (external)
		if err := s.accounts.ReserveHold(ctx, cmd.SourceAccountID, cmd.Currency, amt); err != nil {
			// no hay cambios locales aún
			return err
		}

		// 3.2 crear payment
		p := &dm.Payment{
			ID:             paymentID,
			IdempotencyKey: cmd.IdempotencyKey,
			SourceAccount:  cmd.SourceAccountID,
			DestAccount:    cmd.DestinationAccount,
			Amount:         amt,
			Currency:       cmd.Currency,
			Status:         dm.PaymentPosted,
			CustomerID:     uuidFromStringOrNil(actor.CustomerID),
			CreatedAt:      now,
		}
		if err := w.Payments().Insert(ctx, p); err != nil {
			_ = s.accounts.ReleaseHold(ctx, cmd.SourceAccountID, cmd.Currency, amt)
			return err
		}

		// 3.3 journal entry (doble partida)
		jid := uuid.New()
		// GL accounts: aquí usas IDs/Code desde tu plan de cuentas. Simplifico con códigos.
		// counterparty_ref guarda accountID string para activity.
		srcRef := cmd.SourceAccountID.String()
		dstRef := cmd.DestinationAccount.String()

		lines := []dm.EntryLine{
			{ID: uuid.New(), JournalID: jid, GLAccountID: uuid.New(), GLAccountCode: "GL_OUT", CounterpartyRef: &srcRef, Debit: amt, Credit: decimal.Zero},
			{ID: uuid.New(), JournalID: jid, GLAccountID: uuid.New(), GLAccountCode: "GL_IN", CounterpartyRef: &dstRef, Debit: decimal.Zero, Credit: amt},
		}
		if err := EnsureBalanced(lines); err != nil {
			_ = s.accounts.ReleaseHold(ctx, cmd.SourceAccountID, cmd.Currency, amt)
			return err
		}

		j := &dm.JournalEntry{
			ID:          jid,
			ExternalRef: "payment:" + paymentID.String(),
			BookedAt:    now,
			Status:      dm.JournalPosted,
			Currency:    cmd.Currency,
			CreatedBy:   initiatedBy,
			Lines:       lines,
		}
		if err := w.Journals().InsertJournal(ctx, j); err != nil {
			_ = s.accounts.ReleaseHold(ctx, cmd.SourceAccountID, cmd.Currency, amt)
			return err
		}

		// 3.4 steps
		_ = w.Payments().InsertStep(ctx, &dm.PaymentStep{
			ID: uuid.New(), PaymentID: paymentID,
			Step: "reserve_hold", State: "ok",
			DetailsJSON: `{"account":"` + srcRef + `}`, AttemptedAt: now,
		})
		_ = w.Payments().InsertStep(ctx, &dm.PaymentStep{
			ID: uuid.New(), PaymentID: paymentID,
			Step: "post_ledger", State: "ok",
			DetailsJSON: `{"journal_id":"` + jid.String() + `}`, AttemptedAt: now,
		})

		// 3.5 outbox payment.posted
		payload := map[string]any{
			"payment_id":             paymentID.String(),
			"source_account_id":      srcRef,
			"destination_account_id": dstRef,
			"currency":               cmd.Currency,
			"amount":                 amt.StringFixed(6),
			"occurred_at":            now.Format(time.RFC3339Nano),
		}
		pj, _ := json.Marshal(payload)

		if err := w.Outbox().Insert(ctx, &dm.OutboxEvent{
			ID:            uuid.New(),
			AggregateType: "payment",
			AggregateID:   paymentID,
			EventType:     "payment.posted",
			PayloadJSON:   string(pj),
			Published:     false,
			CreatedAt:     now,
		}); err != nil {
			_ = s.accounts.ReleaseHold(ctx, cmd.SourceAccountID, cmd.Currency, amt)
			return err
		}

		// 3.6 idempotency record
		result = in.PostPaymentResult{PaymentID: paymentID, Status: "posted"}
		rj, _ := json.Marshal(result)
		_ = w.Idempotency().Put(ctx, &model.IdempotencyRecord{
			ID:           uuid.New(),
			Key:          cmd.IdempotencyKey,
			Operation:    "PostPayment",
			ResponseJSON: string(rj),
			StatusCode:   200,
			CreatedAt:    now,
		})

		return nil
	})
	if err != nil {
		// si falla después de reservar hold pero antes de compensar, tu saga puede reintentar release por step
		return in.PostPaymentResult{}, err
	}

	_ = s.audit.Record(ctx, "POST_PAYMENT", "payments", paymentID.String(), initiatedBy, now, map[string]any{
		"currency": cmd.Currency,
		"amount":   amt.StringFixed(6),
	})

	return result, nil
}

func (s *paymentService) GetPayment(ctx context.Context, paymentID uuid.UUID) (in.GetPaymentResult, error) {
	var res in.GetPaymentResult
	err := s.uow.DoRead(ctx, func(r uow.ReadRepos) error {
		p, err := r.Payments().FindById(ctx, paymentID)
		if err != nil {
			return err
		}
		steps, _ := r.Payments().ListSteps(ctx, paymentID)

		res = in.GetPaymentResult{
			PaymentID:        paymentID,
			Status:           string(p.Status),
			IdempotencyKey:   p.IdempotencyKey,
			SourceAccountID:  p.SourceAccount,
			DestAccountID:    p.DestAccount,
			Currency:         p.Currency,
			Amount:           p.Amount.StringFixed(6),
			CreatedAtRFC3339: p.CreatedAt.UTC().Format(time.RFC3339Nano),
		}
		for _, s := range steps {
			res.Steps = append(res.Steps, in.PaymentStepView{
				Step: s.Step, State: s.State,
				DetailsJSON:        s.DetailsJSON,
				AttemptedAtRFC3339: s.AttemptedAt.UTC().Format(time.RFC3339Nano),
			})
		}
		return nil
	})
	return res, err
}

func uuidFromStringOrNil(s string) uuid.UUID {
	u, err := uuid.Parse(s)
	if err != nil {
		return uuid.Nil
	}
	return u
}
