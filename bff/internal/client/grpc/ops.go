package grpc

import (
	"context"
	"time"

	opsv1 "github.com/tagoKoder/bff/internal/client/gen/protobuf/bank/ops/v1"
	"github.com/tagoKoder/bff/internal/client/ports"
	"google.golang.org/grpc"
	"google.golang.org/protobuf/types/known/wrapperspb"
)

var _ ports.OpsPort = (*OpsClient)(nil)

type OpsClient struct {
	audit   opsv1.AuditServiceClient
	notif   opsv1.NotificationsServiceClient
	timeout time.Duration
}

func NewOpsClient(conn *grpc.ClientConn) *OpsClient {
	return &OpsClient{
		audit:   opsv1.NewAuditServiceClient(conn),
		notif:   opsv1.NewNotificationsServiceClient(conn),
		timeout: 5 * time.Second,
	}
}

func (c *OpsClient) GetAuditEventsByTrace(ctx context.Context, in ports.GetAuditEventsByTraceInput) (ports.GetAuditEventsByTraceOutput, error) {
	ctx2, cancel := context.WithTimeout(ctx, c.timeout)
	defer cancel()

	req := &opsv1.GetAuditEventsByTraceRequest{
		TraceId: in.TraceID,
	}
	if in.Limit != nil {
		req.Limit = wrapperspb.Int32(*in.Limit)
	}
	if in.Subject != nil {
		req.XSubject = wrapperspb.String(*in.Subject)
	}

	res, err := c.audit.GetAuditEventsByTrace(ctx2, req)
	if err != nil {
		return ports.GetAuditEventsByTraceOutput{}, err
	}

	out := ports.GetAuditEventsByTraceOutput{Events: make([]ports.AuditEventItem, 0, len(res.Events))}
	for _, e := range res.Events {
		item := ports.AuditEventItem{
			Action:     e.Action,
			ResourceID: e.ResourceId,
		}
		if e.OccurredAt != nil {
			item.OccurredAtRFC3339 = e.OccurredAt.AsTime().Format(time.RFC3339)
		}
		out.Events = append(out.Events, item)
	}
	return out, nil
}

func (c *OpsClient) GetNotificationPrefs(ctx context.Context, in ports.GetNotificationPrefsInput) (ports.GetNotificationPrefsOutput, error) {
	ctx2, cancel := context.WithTimeout(ctx, c.timeout)
	defer cancel()

	req := &opsv1.GetNotificationPrefsRequest{CustomerId: in.CustomerID}
	if in.Subject != nil {
		req.XSubject = wrapperspb.String(*in.Subject)
	}

	res, err := c.notif.GetNotificationPrefs(ctx2, req)
	if err != nil {
		return ports.GetNotificationPrefsOutput{}, err
	}

	out := ports.GetNotificationPrefsOutput{Prefs: make([]ports.NotificationPrefItem, 0, len(res.Prefs))}
	for _, p := range res.Prefs {
		item := ports.NotificationPrefItem{
			Channel: p.Channel.String(),
			OptIn:   p.OptIn,
		}
		if p.UpdatedAt != nil {
			item.UpdatedAtRFC3339 = p.UpdatedAt.AsTime().Format(time.RFC3339)
		}
		out.Prefs = append(out.Prefs, item)
	}
	return out, nil
}
