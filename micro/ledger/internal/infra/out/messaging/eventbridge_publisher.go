// internal/infra/out/messaging/eventbridge_publisher.go
package messaging

import (
	"context"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/eventbridge"
	ebtypes "github.com/aws/aws-sdk-go-v2/service/eventbridge/types"
)

type EventBridgePublisher struct {
	eb      *eventbridge.Client
	busName string
	source  string
}

func NewEventBridgePublisher(eb *eventbridge.Client, busName string) *EventBridgePublisher {
	return &EventBridgePublisher{
		eb: eb, busName: busName,
		source: "bank.ledger",
	}
}

func (p *EventBridgePublisher) Publish(ctx context.Context, eventType, key string, payload []byte) error {
	out, err := p.eb.PutEvents(ctx, &eventbridge.PutEventsInput{
		Entries: []ebtypes.PutEventsRequestEntry{
			{
				EventBusName: aws.String(p.busName),
				Source:       aws.String(p.source),
				DetailType:   aws.String(eventType), // debe ser ledger.journal.posted
				Detail:       aws.String(string(payload)),
			},
		},
	})
	if err != nil {
		return err
	}
	if out.FailedEntryCount != 0 && out.FailedEntryCount > 0 {
		return &TemporaryPublishError{Message: "eventbridge put-events partial failure"}
	}
	return nil
}
