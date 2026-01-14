package messaging

import (
	"context"
	"time"

	"github.com/google/uuid"
	"github.com/tagoKoder/ledger/internal/application/uow"
	out "github.com/tagoKoder/ledger/internal/domain/port/out"
)

type OutboxWorker struct {
	uow       uow.UnitOfWorkManager
	publisher out.EventPublisherPort
	batchSize int
	pollEvery time.Duration
	quit      chan struct{}
}

func NewOutboxWorker(uow uow.UnitOfWorkManager, pub out.EventPublisherPort, batchSize int, pollEvery time.Duration) *OutboxWorker {
	return &OutboxWorker{
		uow: uow, publisher: pub,
		batchSize: batchSize, pollEvery: pollEvery,
		quit: make(chan struct{}),
	}
}

func (w *OutboxWorker) Start() {
	go func() {
		backoff := 200 * time.Millisecond
		for {
			select {
			case <-w.quit:
				return
			default:
			}

			// 1) Fetch (read)
			var events []outboxEventView
			err := w.uow.DoRead(context.Background(), func(r uow.ReadRepos) error {
				es, err := r.Outbox().FetchNextUnpublished(context.Background(), w.batchSize)
				if err != nil {
					return err
				}
				for _, e := range es {
					events = append(events, outboxEventView{
						ID: e.ID, EventType: e.EventType, AggregateID: e.AggregateID, PayloadJSON: e.PayloadJSON,
					})
				}
				return nil
			})
			if err != nil {
				time.Sleep(backoff)
				if backoff < 3*time.Second {
					backoff *= 2
				}
				continue
			}
			if len(events) == 0 {
				backoff = 200 * time.Millisecond
				time.Sleep(w.pollEvery)
				continue
			}

			// 2) Publish outside TX
			var okIDs []uuid.UUID
			for _, e := range events {
				topic := e.EventType // mapping simple
				if err := w.publisher.Publish(context.Background(), topic, e.AggregateID.String(), []byte(e.PayloadJSON)); err != nil {
					// si falla, cortamos (se reintentará luego)
					okIDs = nil
					break
				}
				okIDs = append(okIDs, e.ID)
			}
			if len(okIDs) == 0 {
				time.Sleep(backoff)
				if backoff < 3*time.Second {
					backoff *= 2
				}
				continue
			}

			// 3) MarkPublished (write tx)
			err = w.uow.DoWrite(context.Background(), func(wr uow.WriteRepos) error {
				return wr.Outbox().MarkPublished(context.Background(), okIDs)
			})
			if err != nil {
				time.Sleep(backoff)
				if backoff < 3*time.Second {
					backoff *= 2
				}
				continue
			}

			backoff = 200 * time.Millisecond
			time.Sleep(50 * time.Millisecond)
		}
	}()
}

func (w *OutboxWorker) Stop() { close(w.quit) }

type outboxEventView struct {
	ID          uuid.UUID
	EventType   string
	AggregateID uuid.UUID
	PayloadJSON string
}
