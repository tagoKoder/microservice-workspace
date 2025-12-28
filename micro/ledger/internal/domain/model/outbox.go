package model

import (
	"time"

	"github.com/google/uuid"
)

type OutboxEvent struct {
	ID              uuid.UUID
	AggregateType   string
	AggregateID     uuid.UUID
	EventType       string
	PayloadJSON     string
	Published       bool
	PublishedAt     *time.Time
	Processing      bool
	ProcessingOwner *string
	ProcessingAt    *time.Time
	CreatedAt       time.Time
}
