package service

import (
	"context"
	"encoding/json"
	"time"

	"github.com/google/uuid"
	"github.com/shopspring/decimal"
	"github.com/tagoKoder/ledger/internal/application/uow"
	dm "github.com/tagoKoder/ledger/internal/domain/model"
	in "github.com/tagoKoder/ledger/internal/domain/port/in"
	out "github.com/tagoKoder/ledger/internal/domain/port/out"
)

type ledgerAppService struct {
	uow      uow.UnitOfWorkManager
	accounts out.AccountsGatewayPort
	audit    out.AuditPort
}

func NewLedgerAppService(uow uow.UnitOfWorkManager, accounts out.AccountsGatewayPort, audit out.AuditPort) in.LedgerAppService {
	return &ledgerAppService{uow: uow, accounts: accounts, audit: audit}
}

func (s *ledgerAppService) CreditAccount(ctx context.Context, cmd in.CreditAccountCommand) (in.CreditAccountResult, error) {
	amt, err := decimal.NewFromString(cmd.Amount)
	if err != nil {
		return in.CreditAccountResult{}, err
	}

	// Idempotencia
	var cachedJSON string
	_ = s.uow.DoRead(ctx, func(r uow.ReadRepos) error {
		rec, err := r.Idempotency().Get(ctx, cmd.IdempotencyKey)
		if err != nil {
			return err
		}
		if rec != nil {
			cachedJSON = rec.ResponseJSON
		}
		return nil
	})
	if cachedJSON != "" {
		var prev in.CreditAccountResult
		_ = json.Unmarshal([]byte(cachedJSON), &prev)
		return prev, nil
	}

	// Validación con account (reusa la RPC/endpoint existente)
	if err := s.accounts.ValidateAccountsAndLimits(ctx, cmd.AccountID, cmd.AccountID, cmd.Currency, amt); err != nil {
		return in.CreditAccountResult{}, err
	}

	journalID := uuid.New()
	now := time.Now().UTC()

	var result in.CreditAccountResult
	err = s.uow.DoWrite(ctx, func(w uow.WriteRepos) error {
		// Journal lines:
		// - Débito a "System Funding" (GL_SYSTEM_FUND)
		// - Crédito a cuenta del cliente (GL_CUSTOMER_CASH)
		accRef := cmd.AccountID.String()

		lines := []dm.EntryLine{
			{
				ID: uuid.New(), JournalID: journalID,
				GLAccountID: uuid.New(), GLAccountCode: "GL_SYSTEM_FUND",
				CounterpartyRef: nil,
				Debit:           amt, Credit: decimal.Zero,
			},
			{
				ID: uuid.New(), JournalID: journalID,
				GLAccountID: uuid.New(), GLAccountCode: "GL_CUSTOMER_CASH",
				CounterpartyRef: &accRef,
				Debit:           decimal.Zero, Credit: amt,
			},
		}
		if err := EnsureBalanced(lines); err != nil {
			return err
		}

		j := &dm.JournalEntry{
			ID:          journalID,
			ExternalRef: cmd.ExternalRef,
			BookedAt:    now,
			CreatedBy:   cmd.InitiatedBy,
			Status:      dm.JournalPosted,
			Currency:    cmd.Currency,
			Lines:       lines,
		}
		if err := w.Journals().InsertJournal(ctx, j); err != nil {
			return err
		}

		// Outbox ledger.posted
		payload := map[string]any{
			"journal_id":  journalID.String(),
			"account_id":  cmd.AccountID.String(),
			"currency":    cmd.Currency,
			"amount":      amt.StringFixed(6),
			"occurred_at": now.Format(time.RFC3339Nano),
		}
		pj, _ := json.Marshal(payload)

		if err := w.Outbox().Insert(ctx, &dm.OutboxEvent{
			ID:            uuid.New(),
			AggregateType: "ledger",
			AggregateID:   journalID,
			EventType:     "ledger.posted",
			PayloadJSON:   string(pj),
			Published:     false,
			CreatedAt:     now,
		}); err != nil {
			return err
		}

		// Idempotency save
		result = in.CreditAccountResult{JournalID: journalID, Status: "posted"}
		rj, _ := json.Marshal(result)
		record := &dm.IdempotencyRecord{
			Key:          cmd.IdempotencyKey,
			Operation:    "PostTopup",
			ResponseJSON: string(rj),
			StatusCode:   200,
			CreatedAt:    now,
		}
		if err := w.Idempotency().Put(ctx, record); err != nil {
			return err
		}
		return nil
	})
	if err != nil {
		return in.CreditAccountResult{}, err
	}

	_ = s.audit.Record(ctx, "POST_TOPUP", "journal_entries", journalID.String(), cmd.InitiatedBy, now, map[string]any{
		"account_id": cmd.AccountID.String(),
		"currency":   cmd.Currency,
		"amount":     amt.StringFixed(6),
	})

	return result, nil
}

func (s *ledgerAppService) ListAccountJournalEntries(ctx context.Context, q in.ListAccountJournalEntriesQuery) (in.ListAccountJournalEntriesResult, error) {
	from := time.Time{}
	to := time.Now().UTC()

	if q.FromRFC3339 != "" {
		t, err := time.Parse(time.RFC3339Nano, q.FromRFC3339)
		if err == nil {
			from = t
		}
	}
	if q.ToRFC3339 != "" {
		t, err := time.Parse(time.RFC3339Nano, q.ToRFC3339)
		if err == nil {
			to = t
		}
	}

	if q.Page <= 0 {
		q.Page = 1
	}
	if q.Size <= 0 {
		q.Size = 20
	}

	var entries []dm.JournalEntry
	err := s.uow.DoRead(ctx, func(r uow.ReadRepos) error {
		var err error
		entries, err = r.Journals().ListActivityByAccount(ctx, q.AccountID, from, to, q.Page, q.Size)
		return err
	})
	if err != nil {
		return in.ListAccountJournalEntriesResult{}, err
	}

	views := make([]in.JournalEntryView, 0, len(entries))
	for _, e := range entries {
		v := in.JournalEntryView{
			JournalID:       e.ID.String(),
			Currency:        e.Currency,
			Status:          string(e.Status),
			BookedAtRFC3339: e.BookedAt.UTC().Format(time.RFC3339Nano),
			ExternalRef:     e.ExternalRef,
		}
		for _, l := range e.Lines {
			cp := ""
			if l.CounterpartyRef != nil {
				cp = *l.CounterpartyRef
			}
			v.Lines = append(v.Lines, in.JournalLineView{
				GLAccountCode:   l.GLAccountCode,
				CounterpartyRef: cp,
				Debit:           l.Debit.StringFixed(6),
				Credit:          l.Credit.StringFixed(6),
			})
		}
		views = append(views, v)
	}

	return in.ListAccountJournalEntriesResult{Entries: views, Page: q.Page, Size: q.Size}, nil
}

func (s *ledgerAppService) CreateManualJournalEntry(ctx context.Context, cmd in.CreateManualJournalEntryCommand) (in.CreateManualJournalEntryResult, error) {
	bookedAt := time.Now().UTC()
	if cmd.BookedAtRFC3339 != "" {
		t, err := time.Parse(time.RFC3339Nano, cmd.BookedAtRFC3339)
		if err == nil {
			bookedAt = t
		}
	}

	jid := uuid.New()

	// Map cmd lines -> domain lines (decimal)
	var lines []dm.EntryLine
	for _, l := range cmd.Lines {
		d, err := decimal.NewFromString(l.Debit)
		if err != nil {
			return in.CreateManualJournalEntryResult{}, err
		}
		c, err := decimal.NewFromString(l.Credit)
		if err != nil {
			return in.CreateManualJournalEntryResult{}, err
		}
		cp := l.CounterpartyRef
		var cpPtr *string
		if cp != "" {
			cpPtr = &cp
		}

		lines = append(lines, dm.EntryLine{
			ID: uuid.New(), JournalID: jid,
			GLAccountID:     uuid.New(), // idealmente lo resuelves por code->id leyendo gl_accounts
			GLAccountCode:   l.GLAccountCode,
			CounterpartyRef: cpPtr,
			Debit:           d, Credit: c,
		})
	}
	if err := EnsureBalanced(lines); err != nil {
		return in.CreateManualJournalEntryResult{}, err
	}

	now := time.Now().UTC()
	err := s.uow.DoWrite(ctx, func(w uow.WriteRepos) error {
		j := &dm.JournalEntry{
			ID:          jid,
			ExternalRef: cmd.ExternalRef,
			BookedAt:    bookedAt,
			CreatedBy:   cmd.CreatedBy,
			Status:      dm.JournalPosted,
			Currency:    cmd.Currency,
			Lines:       lines,
		}
		if err := w.Journals().InsertJournal(ctx, j); err != nil {
			return err
		}

		payload := map[string]any{
			"journal_id":  jid.String(),
			"currency":    cmd.Currency,
			"occurred_at": now.Format(time.RFC3339Nano),
		}
		pj, _ := json.Marshal(payload)

		return w.Outbox().Insert(ctx, &dm.OutboxEvent{
			ID: uuid.New(), AggregateType: "ledger", AggregateID: jid,
			EventType: "ledger.posted", PayloadJSON: string(pj),
			Published: false, CreatedAt: now,
		})
	})
	if err != nil {
		return in.CreateManualJournalEntryResult{}, err
	}

	_ = s.audit.Record(ctx, "CREATE_JOURNAL_ENTRY", "journal_entries", jid.String(), cmd.CreatedBy, now, map[string]any{
		"currency": cmd.Currency,
		"lines":    len(lines),
	})

	return in.CreateManualJournalEntryResult{JournalID: jid, Status: "posted"}, nil
}
