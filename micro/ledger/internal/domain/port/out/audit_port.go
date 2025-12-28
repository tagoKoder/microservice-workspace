package out

import (
	"context"
	"time"
)

type AuditPort interface {
	Record(ctx context.Context, action, entity, entityID, actor string, at time.Time, details map[string]any) error
}
