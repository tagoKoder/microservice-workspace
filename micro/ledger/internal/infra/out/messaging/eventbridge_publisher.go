// micro\ledger\internal\infra\out\messaging\eventbridge_publisher.go
package messaging

import (
	"context"
	"time"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/eventbridge"
	ebtypes "github.com/aws/aws-sdk-go-v2/service/eventbridge/types"
)

type EventBridgePublisher struct {
	eb      *eventbridge.Client
	busName string
	source  string
}

func NewEventBridgePublisher(eb *eventbridge.Client, busName, source string) *EventBridgePublisher {
	if source == "" {
		source = "bank.ledger"
	}
	return &EventBridgePublisher{eb: eb, busName: busName, source: source}
}

func (p *EventBridgePublisher) Publish(ctx context.Context, eventType, key string, payload []byte) error {
	entry := ebtypes.PutEventsRequestEntry{
		Source:     aws.String(p.source),
		DetailType: aws.String(eventType),       // e.g. ledger.journal.posted
		Detail:     aws.String(string(payload)), // JSON
		Time:       aws.Time(time.Now().UTC()),
	}
	if p.busName != "" {
		entry.EventBusName = aws.String(p.busName)
	}

	out, err := p.eb.PutEvents(ctx, &eventbridge.PutEventsInput{Entries: []ebtypes.PutEventsRequestEntry{entry}})
	if err != nil {
		return &TemporaryPublishError{Message: "eventbridge put-events error", Code: "api_error"}
	}
	if out.FailedEntryCount > 0 {
		// inspecciona entry result
		if len(out.Entries) > 0 && out.Entries[0].ErrorCode != nil {
			code := *out.Entries[0].ErrorCode
			return &TemporaryPublishError{Message: "eventbridge put-events failed entry", Code: code}
		}
		return &TemporaryPublishError{Message: "eventbridge put-events partial failure", Code: "partial_failure"}
	}
	return nil
}
