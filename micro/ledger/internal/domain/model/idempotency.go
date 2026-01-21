// micro\ledger\internal\domain\model\idempotency.go
package model

import (
	"time"

	"github.com/google/uuid"
)

type IdempotencyRecord struct {
	ID           uuid.UUID
	Key          string
	Operation    string
	ResponseJSON string
	StatusCode   int
	CreatedAt    time.Time
}
