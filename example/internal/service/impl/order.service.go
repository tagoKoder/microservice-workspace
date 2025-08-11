package services

import (
	"context"
	"fmt"

	"github.com/tagoKoder/clinic/internal/domain"
	"github.com/tagoKoder/clinic/internal/ports"
)

type OrderService struct {
	tx ports.TxManager
}

func NewOrderService(tx ports.TxManager) *OrderService {
	return &OrderService{tx: tx}
}

type PlaceOrderInput struct {
	UserID string
	Items  []struct {
		SKU       string
		Qty       int
		UnitPrice int64
	}
}

// ACID local: todo lo que pase en esta función se confirma o revierte junto.
func (s *OrderService) PlaceOrder(ctx context.Context, in PlaceOrderInput) (string, error) {
	var orderID string

	err := s.tx.Do(ctx, func(uow ports.UnitOfWork) error {
		// 1) construir agregado de dominio
		items := make([]domain.OrderItem, 0, len(in.Items))
		for _, it := range in.Items {
			items = append(items, domain.OrderItem{
				SKU:       it.SKU,
				Qty:       it.Qty,
				UnitPrice: it.UnitPrice,
			})
		}
		order, err := domain.NewOrder(in.UserID, items)
		if err != nil {
			return err
		}

		// 2) persistir (Order + Items) en la MISMA TX
		if err := uow.Orders().Save(ctx, order); err != nil {
			return err
		}
		orderID = order.ID

		// 3) (opcional) otras escrituras locales con otros repos… todas ACID

		// Simular una validación que si falla fuerza rollback
		if len(order.Items) == 0 {
			return fmt.Errorf("empty order")
		}
		return nil
	})

	return orderID, err
}

type CancelOrderInput struct {
	OrderID string
}

func (s *OrderService) CancelOrder(ctx context.Context, in CancelOrderInput) error {
	return s.tx.Do(ctx, func(uow ports.UnitOfWork) error {
		o, err := uow.Orders().GetByID(ctx, in.OrderID)
		if err != nil {
			return err
		}
		if o == nil {
			return fmt.Errorf("order %s not found", in.OrderID)
		}
		if err := o.Cancel(); err != nil {
			return err
		}
		return uow.Orders().Save(ctx, o)
	})
}
