package messaging

import (
	"context"
	"time"

	kafka "github.com/segmentio/kafka-go"
	"github.com/sony/gobreaker"
)

type KafkaPublisher struct {
	writer *kafka.Writer
	cb     *gobreaker.CircuitBreaker
}

func NewKafkaPublisher(brokers []string, clientID string) *KafkaPublisher {
	w := &kafka.Writer{
		Addr:         kafka.TCP(brokers...),
		Balancer:     &kafka.Hash{},
		RequiredAcks: kafka.RequireOne,
		Async:        false,
		BatchTimeout: 10 * time.Millisecond,
	}
	cb := gobreaker.NewCircuitBreaker(gobreaker.Settings{
		Name:        "kafka",
		ReadyToTrip: func(c gobreaker.Counts) bool { return c.ConsecutiveFailures >= 3 },
		Timeout:     10 * time.Second,
	})
	return &KafkaPublisher{writer: w, cb: cb}
}

func (p *KafkaPublisher) Publish(ctx context.Context, topic, key string, payload []byte) error {
	_, err := p.cb.Execute(func() (any, error) {
		return nil, p.writer.WriteMessages(ctx, kafka.Message{
			Topic: topic,
			Key:   []byte(key),
			Value: payload,
		})
	})
	return err
}
