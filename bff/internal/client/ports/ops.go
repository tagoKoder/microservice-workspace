package ports

import "context"

type GetAuditEventsByTraceInput struct {
	TraceID string
	Limit   *int32
	Subject *string
}
type AuditEventItem struct {
	Action            string
	OccurredAtRFC3339 string
	ResourceID        string
}
type GetAuditEventsByTraceOutput struct {
	Events []AuditEventItem
}

type GetNotificationPrefsInput struct {
	CustomerID string
	Subject    *string
}
type NotificationPrefItem struct {
	Channel          string
	OptIn            bool
	UpdatedAtRFC3339 string
}
type GetNotificationPrefsOutput struct {
	Prefs []NotificationPrefItem
}

type OpsPort interface {
	GetAuditEventsByTrace(ctx context.Context, in GetAuditEventsByTraceInput) (GetAuditEventsByTraceOutput, error)
	GetNotificationPrefs(ctx context.Context, in GetNotificationPrefsInput) (GetNotificationPrefsOutput, error)
}
