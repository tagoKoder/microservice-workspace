// micro\ledger\internal\infra\out\persistence\gorm\adapter\journal_repo.go
package adapter

import (
	"context"
	"time"

	"github.com/google/uuid"
	dm "github.com/tagoKoder/ledger/internal/domain/model"
	out "github.com/tagoKoder/ledger/internal/domain/port/out"
	"github.com/tagoKoder/ledger/internal/infra/out/persistence/gorm/entity"
	"gorm.io/gorm"
)

type JournalRepo struct{ db *gorm.DB }

func NewJournalRepo(db *gorm.DB) out.JournalRepositoryPort { return &JournalRepo{db: db} }

func (r *JournalRepo) InsertJournal(ctx context.Context, j *dm.JournalEntry) error {
	m := entity.JournalEntryEntity{
		ID:          j.ID,
		ExternalRef: j.ExternalRef,
		BookedAt:    j.BookedAt,
		CreatedBy:   j.CreatedBy,
		Status:      string(j.Status),
		Currency:    j.Currency,
	}

	lines := make([]entity.EntryLineEntity, 0, len(j.Lines))
	for _, l := range j.Lines {
		lines = append(lines, entity.EntryLineEntity{
			ID:              l.ID,
			JournalID:       j.ID,
			GLAccountID:     l.GLAccountID,
			GLAccountCode:   l.GLAccountCode,
			CounterpartyRef: l.CounterpartyRef,
			Debit:           l.Debit,
			Credit:          l.Credit,
		})
	}

	return r.db.WithContext(ctx).Transaction(func(tx *gorm.DB) error {
		if err := tx.Create(&m).Error; err != nil {
			return err
		}
		if len(lines) > 0 {
			if err := tx.Create(&lines).Error; err != nil {
				return err
			}
		}
		return nil
	})
}

func (r *JournalRepo) GetJournalByID(ctx context.Context, id uuid.UUID) (*dm.JournalEntry, error) {
	var m entity.JournalEntryEntity
	if err := r.db.WithContext(ctx).Preload("Lines").First(&m, "id = ?", id).Error; err != nil {
		return nil, err
	}

	j := &dm.JournalEntry{
		ID:          m.ID,
		ExternalRef: m.ExternalRef,
		BookedAt:    m.BookedAt,
		CreatedBy:   m.CreatedBy,
		Status:      dm.JournalStatus(m.Status),
		Currency:    m.Currency,
	}
	for _, l := range m.Lines {
		j.Lines = append(j.Lines, dm.EntryLine{
			ID:              l.ID,
			JournalID:       l.JournalID,
			GLAccountID:     l.GLAccountID,
			GLAccountCode:   l.GLAccountCode,
			CounterpartyRef: l.CounterpartyRef,
			Debit:           l.Debit,
			Credit:          l.Credit,
		})
	}
	return j, nil
}

func (r *JournalRepo) ListActivityByAccount(ctx context.Context, accountID uuid.UUID, from, to time.Time, page, size int) ([]dm.JournalEntry, error) {
	if size <= 0 {
		size = 20
	}
	if page < 0 {
		page = 0
	}
	offset := page * size

	// Heurística: match por counterparty_ref = accountID
	// (en tu diseño original es "counterparty_ref text"; aquí guardamos el UUID string del account)
	var js []entity.JournalEntryEntity
	err := r.db.WithContext(ctx).
		Joins("JOIN entry_lines el ON el.journal_id = journal_entries.id").
		Where(`
			el.counterparty_ref = ?
			AND journal_entries.booked_at >= ?
			AND journal_entries.booked_at <= ?
		`, accountID.String(), from, to).
		Order("journal_entries.booked_at desc").
		Limit(size).Offset(offset).
		Preload("Lines").
		Find(&js).Error
	if err != nil {
		return nil, err
	}

	out := make([]dm.JournalEntry, 0, len(js))
	for _, m := range js {
		j := dm.JournalEntry{
			ID:          m.ID,
			ExternalRef: m.ExternalRef,
			BookedAt:    m.BookedAt,
			CreatedBy:   m.CreatedBy,
			Status:      dm.JournalStatus(m.Status),
			Currency:    m.Currency,
		}
		for _, l := range m.Lines {
			j.Lines = append(j.Lines, dm.EntryLine{
				ID:              l.ID,
				JournalID:       l.JournalID,
				GLAccountID:     l.GLAccountID,
				GLAccountCode:   l.GLAccountCode,
				CounterpartyRef: l.CounterpartyRef,
				Debit:           l.Debit,
				Credit:          l.Credit,
			})
		}
		out = append(out, j)
	}
	return out, nil
}
