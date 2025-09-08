package impl

import (
	"context"

	"github.com/tagoKoder/clinic/internal/domain/model"
	"github.com/tagoKoder/clinic/internal/repository/uow"
)

// BusinessService orquesta casos de uso del agregado Business.
// No conoce GORM ni DB; solo depende de TxManager + UnitOfWork.
type BusinessService struct {
	tx    uow.TxManager
	query uow.QueryManager
}

// NewBusinessService construye el servicio.
func NewBusinessService(tx uow.TxManager, query uow.QueryManager) *BusinessService {
	return &BusinessService{tx: tx, query: query}
}

// ---------- DTOs de entrada ----------

type CreateBusinessInput struct {
	Name         string
	GovernmentID string
}

type UpdateBusinessInput struct {
	ID   int64
	Name string
}

// ------ READ --------

// READ (va al QueryManager -> readDB o primario si aún no hay replica)
func (s *BusinessService) GetByID(ctx context.Context, id int64) (*model.Business, error) {
	var out *model.Business
	err := s.query.Do(ctx, func(q uow.QueryWork) error {
		var err error
		out, err = q.Businesses().GetByID(ctx, id)
		return err
	})
	return out, err
}

// GetByGovernmentID: trae un Business por government_id (nil si no existe).
func (s *BusinessService) GetByGovernmentID(ctx context.Context, govID string) (*model.Business, error) {
	var out *model.Business
	err := s.query.Do(ctx, func(q uow.QueryWork) error {
		var err error
		out, err = q.Businesses().GetByGovernmentID(ctx, govID)
		return err
	})
	return out, err
}

// ------ WRITE ------

// Create: crea un Business y retorna su ID (ACID local).
func (s *BusinessService) Create(ctx context.Context, in CreateBusinessInput) (int64, error) {
	var id int64
	err := s.tx.Do(ctx, func(uow uow.UnitOfWork) error {
		m := &model.Business{
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
	return s.tx.Do(ctx, func(uow uow.UnitOfWork) error {
		m := &model.Business{
			ID:   in.ID,
			Name: in.Name,
		}
		return uow.Businesses().UpdateCore(ctx, m)
	})
}

// SoftDelete: marca como eliminado (soft delete).
func (s *BusinessService) SoftDelete(ctx context.Context, id int64) error {
	return s.tx.Do(ctx, func(uow uow.UnitOfWork) error {
		return uow.Businesses().SoftDelete(ctx, id)
	})
}

// Restore: revierte el soft delete.
func (s *BusinessService) Restore(ctx context.Context, id int64) error {
	return s.tx.Do(ctx, func(uow uow.UnitOfWork) error {
		return uow.Businesses().Restore(ctx, id)
	})
}
