package out

import "context"

type EventPublisherPort interface {
	Publish(ctx context.Context, topic string, key string, payload []byte) error
}
