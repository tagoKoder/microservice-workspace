// micro\ledger\internal\infra\out\persistence\gorm\uow\uow.go
package uow

import (
	"context"

	"github.com/tagoKoder/ledger/internal/application/uow"
	out "github.com/tagoKoder/ledger/internal/domain/port/out"
	"github.com/tagoKoder/ledger/internal/infra/out/persistence/gorm/adapter"
	"gorm.io/gorm"
)

type gormUnitOfWork struct {
	readDB  *gorm.DB
	writeDB *gorm.DB
}

func NewGormUnitOfWork(readDB, writeDB *gorm.DB) uow.UnitOfWorkManager {
	return &gormUnitOfWork{readDB: readDB, writeDB: writeDB}
}

func (m *gormUnitOfWork) DoWrite(ctx context.Context, fn func(uow.WriteRepos) error) error {
	return m.writeDB.WithContext(ctx).Transaction(func(tx *gorm.DB) error {
		repos := newGormWriteRepos(tx)
		return fn(repos)
	})
}

func (m *gormUnitOfWork) DoRead(ctx context.Context, fn func(uow.ReadRepos) error) error {
	repos := newGormReadRepos(m.readDB.WithContext(ctx))
	return fn(repos)
}

type gormWriteRepos struct{ tx *gorm.DB }
type gormReadRepos struct{ db *gorm.DB }

func newGormWriteRepos(tx *gorm.DB) uow.WriteRepos { return &gormWriteRepos{tx: tx} }
func newGormReadRepos(db *gorm.DB) uow.ReadRepos   { return &gormReadRepos{db: db} }

func (r *gormWriteRepos) Payments() out.PaymentRepositoryPort { return adapter.NewPaymentRepo(r.tx) }
func (r *gormWriteRepos) Journals() out.JournalRepositoryPort { return adapter.NewJournalRepo(r.tx) }
func (r *gormWriteRepos) Outbox() out.OutboxRepositoryPort    { return adapter.NewOutboxRepo(r.tx) }
func (r *gormWriteRepos) Idempotency() out.IdempotencyRepositoryPort {
	return adapter.NewIdempotencyRepo(r.tx)
}

func (r *gormReadRepos) Payments() out.PaymentRepositoryPort { return adapter.NewPaymentRepo(r.db) }
func (r *gormReadRepos) Journals() out.JournalRepositoryPort { return adapter.NewJournalRepo(r.db) }
func (r *gormReadRepos) Outbox() out.OutboxRepositoryPort    { return adapter.NewOutboxRepo(r.db) }
func (r *gormReadRepos) Idempotency() out.IdempotencyRepositoryPort {
	return adapter.NewIdempotencyRepo(r.db)
}
