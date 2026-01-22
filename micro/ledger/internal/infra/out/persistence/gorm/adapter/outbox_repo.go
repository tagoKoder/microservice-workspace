package adapter

import (
	"context"
	"os"
	"time"

	"github.com/google/uuid"
	dm "github.com/tagoKoder/ledger/internal/domain/model"
	out "github.com/tagoKoder/ledger/internal/domain/port/out"
	"github.com/tagoKoder/ledger/internal/infra/out/persistence/gorm/entity"
	"gorm.io/gorm"
)

type OutboxRepo struct{ db *gorm.DB }

func NewOutboxRepo(db *gorm.DB) out.OutboxRepositoryPort { return &OutboxRepo{db: db} }

func (r *OutboxRepo) Insert(ctx context.Context, e *dm.OutboxEvent) error {
	m := entity.OutboxEventEntity{
		ID:            e.ID,
		AggregateType: e.AggregateType,
		AggregateID:   e.AggregateID,
		EventType:     e.EventType,
		PayloadJSON:   e.PayloadJSON,
		Published:     e.Published,
		CreatedAt:     e.CreatedAt,
	}
	return r.db.WithContext(ctx).Create(&m).Error
}

func (r *OutboxRepo) FetchNextUnpublished(ctx context.Context, limit int) ([]dm.OutboxEvent, error) {
	if limit <= 0 {
		limit = 50
	}

	var claimed []entity.OutboxEventEntity
	now := time.Now().UTC()
	ttl := 2 * time.Minute // claim TTL mínimo para destrabar

	owner := os.Getenv("HOSTNAME")
	if owner == "" {
		owner = "unknown"
	}

	err := r.db.WithContext(ctx).Transaction(func(tx *gorm.DB) error {
		var ids []uuid.UUID

		// TABLA CORRECTA: outbox_events (no outbox_event_entities)
		if err := tx.Raw(`
			SELECT id
			FROM outbox_events
			WHERE published = false
			  AND (
			       processing = false
			       OR (processing = true AND processing_at < ?)
			  )
			ORDER BY created_at ASC
			LIMIT ?
			FOR UPDATE SKIP LOCKED
		`, now.Add(-ttl), limit).Scan(&ids).Error; err != nil {
			return err
		}
		if len(ids) == 0 {
			claimed = nil
			return nil
		}

		if err := tx.Model(&entity.OutboxEventEntity{}).
			Where("id IN ?", ids).
			Updates(map[string]any{
				"processing":       true,
				"processing_owner": owner,
				"processing_at":    now,
			}).Error; err != nil {
			return err
		}

		return tx.Where("id IN ?", ids).
			Order("created_at ASC").
			Find(&claimed).Error
	})
	if err != nil {
		return nil, err
	}

	outEvents := make([]dm.OutboxEvent, 0, len(claimed))
	for _, m := range claimed {
		outEvents = append(outEvents, dm.OutboxEvent{
			ID:            m.ID,
			AggregateType: m.AggregateType,
			AggregateID:   m.AggregateID,
			EventType:     m.EventType,
			PayloadJSON:   m.PayloadJSON,
			Published:     m.Published,
			CreatedAt:     m.CreatedAt,
		})
	}
	return outEvents, nil
}

func (r *OutboxRepo) MarkPublished(ctx context.Context, ids []uuid.UUID) error {
	if len(ids) == 0 {
		return nil
	}
	now := time.Now().UTC()
	return r.db.WithContext(ctx).Model(&entity.OutboxEventEntity{}).
		Where("id IN ?", ids).
		Updates(map[string]any{
			"published":        true,
			"published_at":     now,
			"processing":       false,
			"processing_owner": nil,
			"processing_at":    nil,
		}).Error
}
