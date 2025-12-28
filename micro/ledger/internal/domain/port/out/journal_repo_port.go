package out

import (
	"context"
	"time"

	"github.com/google/uuid"
	"github.com/tagoKoder/ledger/internal/domain/model"
)

type JournalRepositoryPort interface {
	InsertJournal(ctx context.Context, j *model.JournalEntry) error
	GetJournalByID(ctx context.Context, id uuid.UUID) (*model.JournalEntry, error)
	ListActivityByAccount(ctx context.Context, accountID uuid.UUID, from, to time.Time, page, size int) ([]model.JournalEntry, error)
}
