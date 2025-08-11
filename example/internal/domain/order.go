package domain

import (
	"errors"
	"time"
)

type OrderStatus string

const (
	OrderPending  OrderStatus = "PENDING"
	OrderPaid     OrderStatus = "PAID"
	OrderCanceled OrderStatus = "CANCELED"
)

type OrderItem struct {
	ID        string
	OrderID   string
	SKU       string
	Qty       int
	UnitPrice int64 // centavos
	CreatedAt time.Time
	UpdatedAt time.Time
}

type Order struct {
	ID        string
	UserID    string
	Status    OrderStatus
	Items     []OrderItem
	CreatedAt time.Time
	UpdatedAt time.Time
}

func NewOrder(userID string, items []OrderItem) (*Order, error) {
	if userID == "" {
		return nil, errors.New("userID is required")
	}
	if len(items) == 0 {
		return nil, errors.New("at least one item required")
	}
	o := &Order{
		UserID: userID,
		Status: OrderPending,
		Items:  items,
	}
	return o, nil
}

func (o *Order) Cancel() error {
	if o.Status == OrderCanceled {
		return nil
	}
	if o.Status == OrderPaid {
		return errors.New("paid orders cannot be canceled")
	}
	o.Status = OrderCanceled
	return nil
}
