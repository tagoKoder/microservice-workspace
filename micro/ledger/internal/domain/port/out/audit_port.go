package out

import (
	"context"
	"time"
)

type AuditPort interface {
	Record(ctx context.Context, entity, entityID string, at time.Time, details map[string]any) error
}
