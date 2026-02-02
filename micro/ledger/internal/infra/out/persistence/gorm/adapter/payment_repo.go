// micro\ledger\internal\infra\out\persistence\gorm\adapter\payment_repo.go
package adapter

import (
	"context"
	"errors"
	"time"

	"github.com/google/uuid"
	dm "github.com/tagoKoder/ledger/internal/domain/model"
	out "github.com/tagoKoder/ledger/internal/domain/port/out"
	"github.com/tagoKoder/ledger/internal/infra/out/persistence/gorm/entity"
	"gorm.io/gorm"
)

type PaymentRepo struct{ db *gorm.DB }

func NewPaymentRepo(db *gorm.DB) out.PaymentRepositoryPort { return &PaymentRepo{db: db} }

func (r *PaymentRepo) FindById(ctx context.Context, id uuid.UUID) (*dm.Payment, error) {
	var m entity.PaymentEntity
	if err := r.db.WithContext(ctx).Preload("Steps").First(&m, "id = ?", id).Error; err != nil {
		return nil, err
	}
	return toDomainPayment(&m), nil
}

func (r *PaymentRepo) FindByIdempotencyKey(ctx context.Context, key string) (*dm.Payment, error) {
	var m entity.PaymentEntity
	err := r.db.WithContext(ctx).Preload("Steps").First(&m, "idempotency_key = ?", key).Error
	if errors.Is(err, gorm.ErrRecordNotFound) {
		// Si prefieres "nil, nil" cámbialo, pero mantengo tu patrón actual.
		return &dm.Payment{}, nil
	}
	if err != nil {
		return nil, err
	}
	return toDomainPayment(&m), nil
}

func (r *PaymentRepo) Insert(ctx context.Context, p *dm.Payment) error {
	m := fromDomainPayment(p)
	return r.db.WithContext(ctx).Create(&m).Error
}

func (r *PaymentRepo) InsertStep(ctx context.Context, step *dm.PaymentStep) error {
	m := entity.PaymentStepEntity{
		ID:          step.ID,
		PaymentID:   step.PaymentID,
		Step:        step.Step,
		State:       step.State,
		DetailsJSON: step.DetailsJSON,
		AttemptedAt: step.AttemptedAt,
	}
	return r.db.WithContext(ctx).Create(&m).Error
}

func (r *PaymentRepo) UpdateStatus(ctx context.Context, id uuid.UUID, status string) error {
	return r.db.WithContext(ctx).Model(&entity.PaymentEntity{}).
		Where("id = ?", id).
		Updates(map[string]any{
			"status":     status,
			"updated_at": time.Now().UTC(),
		}).Error
}

func (r *PaymentRepo) ListSteps(ctx context.Context, paymentID uuid.UUID) ([]dm.PaymentStep, error) {
	var ms []entity.PaymentStepEntity
	if err := r.db.WithContext(ctx).
		Order("attempted_at asc").
		Find(&ms, "payment_id = ?", paymentID).Error; err != nil {
		return nil, err
	}

	stepsOut := make([]dm.PaymentStep, 0, len(ms))
	for _, x := range ms {
		stepsOut = append(stepsOut, dm.PaymentStep{
			ID:          x.ID,
			PaymentID:   x.PaymentID,
			Step:        x.Step,
			State:       x.State,
			DetailsJSON: x.DetailsJSON,
			AttemptedAt: x.AttemptedAt,
		})
	}
	return stepsOut, nil
}

func toDomainPayment(m *entity.PaymentEntity) *dm.Payment {
	return &dm.Payment{
		ID:             m.ID,
		IdempotencyKey: m.IdempotencyKey,
		SourceAccount:  m.SourceAccount,
		DestAccount:    m.DestAccount,
		Amount:         m.Amount,
		Currency:       m.Currency,
		Status:         dm.PaymentStatus(m.Status),

		CustomerID:    m.CustomerID,
		HoldID:        m.HoldID,
		JournalID:     m.JournalID,
		CorrelationID: m.CorrelationID,
		CreatedAt:     m.CreatedAt,
		UpdatedAt:     m.UpdatedAt,
	}
}

func fromDomainPayment(p *dm.Payment) entity.PaymentEntity {
	return entity.PaymentEntity{
		ID:             p.ID,
		IdempotencyKey: p.IdempotencyKey,
		SourceAccount:  p.SourceAccount,
		DestAccount:    p.DestAccount,
		Amount:         p.Amount,
		Currency:       p.Currency,
		Status:         string(p.Status),

		CustomerID:    p.CustomerID,
		HoldID:        p.HoldID,
		JournalID:     p.JournalID,
		CorrelationID: p.CorrelationID,
		CreatedAt:     p.CreatedAt,
		UpdatedAt:     p.UpdatedAt,
	}
}

var ErrNotFound = errors.New("not found")

func isNotFound(err error) bool {
	return errors.Is(err, gorm.ErrRecordNotFound)
}
