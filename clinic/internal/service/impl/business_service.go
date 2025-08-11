package impl

import (
	"context"

	"github.com/tagoKoder/clinic/internal/adapter/db"
	"github.com/tagoKoder/clinic/internal/domain/model"
)

// BusinessService orquesta casos de uso del agregado Business.
// No conoce GORM ni DB; solo depende de TxManager + UnitOfWork.
type BusinessService struct {
	tx db.TxManager
}

// NewBusinessService construye el servicio.
func NewBusinessService(tx db.TxManager) *BusinessService {
	return &BusinessService{tx: tx}
}

// ---------- DTOs de entrada ----------

type CreateBusinessInput struct {
	TimeZoneID   int64
	Name         string
	GovernmentID string
}

type UpdateBusinessInput struct {
	ID         int64
	Name       string
	TimeZoneID int64
}

// ---------- Casos de uso ----------

// Create: crea un Business y retorna su ID (ACID local).
func (s *BusinessService) Create(ctx context.Context, in CreateBusinessInput) (int64, error) {
	var id int64
	err := s.tx.Do(ctx, func(uow db.UnitOfWork) error {
		m := &model.Business{
			TimeZoneID:   in.TimeZoneID,
			Name:         in.Name,
			GovernmentID: in.GovernmentID,
		}
		if err := uow.Businesses().Create(ctx, m); err != nil {
			return err
		}
		id = m.ID
		return nil
	})
	return id, err
}

// UpdateCore: actualiza campos “core” (Name, TimeZoneID).
func (s *BusinessService) UpdateCore(ctx context.Context, in UpdateBusinessInput) error {
	return s.tx.Do(ctx, func(uow db.UnitOfWork) error {
		m := &model.Business{
			ID:         in.ID,
			Name:       in.Name,
			TimeZoneID: in.TimeZoneID,
		}
		return uow.Businesses().UpdateCore(ctx, m)
	})
}

// GetByID: trae un Business por ID (nil si no existe).
func (s *BusinessService) GetByID(ctx context.Context, id int64) (*model.Business, error) {
	var out *model.Business
	err := s.tx.Do(ctx, func(uow db.UnitOfWork) error {
		var err error
		out, err = uow.Businesses().GetByID(ctx, id)
		return err
	})
	return out, err
}

// GetByGovernmentID: trae un Business por government_id (nil si no existe).
func (s *BusinessService) GetByGovernmentID(ctx context.Context, govID string) (*model.Business, error) {
	var out *model.Business
	err := s.tx.Do(ctx, func(uow db.UnitOfWork) error {
		var err error
		out, err = uow.Businesses().GetByGovernmentID(ctx, govID)
		return err
	})
	return out, err
}

// SoftDelete: marca como eliminado (soft delete).
func (s *BusinessService) SoftDelete(ctx context.Context, id int64) error {
	return s.tx.Do(ctx, func(uow db.UnitOfWork) error {
		return uow.Businesses().SoftDelete(ctx, id)
	})
}

// Restore: revierte el soft delete.
func (s *BusinessService) Restore(ctx context.Context, id int64) error {
	return s.tx.Do(ctx, func(uow db.UnitOfWork) error {
		return uow.Businesses().Restore(ctx, id)
	})
}
