package uow

import "github.com/tagoKoder/ledger/internal/domain/port/out"

type WriteRepos interface {
	Payments() out.PaymentRepositoryPort
	Journals() out.JournalRepositoryPort
	Outbox() out.OutboxRepositoryPort
	Idempotency() out.IdempotencyRepositoryPort
}

type ReadRepos interface {
	Payments() out.PaymentRepositoryPort
	Journals() out.JournalRepositoryPort
	Outbox() out.OutboxRepositoryPort
	Idempotency() out.IdempotencyRepositoryPort
}
