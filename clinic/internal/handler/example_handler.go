package handler

import (
	"context"
	"fmt"
	"log/slog"
	"strings"

	"go.opentelemetry.io/otel/attribute"
	"go.opentelemetry.io/otel/trace"

	"github.com/tagoKoder/clinic/internal/adapter/logger"
	examplepb "github.com/tagoKoder/proto/genproto/go/example"
	"google.golang.org/protobuf/types/known/timestamppb"
)

type ExampleHandler struct {
	examplepb.UnimplementedExampleServiceServer
}

func NewExampleHandler() *ExampleHandler { return &ExampleHandler{} }

func (h *ExampleHandler) GetExample(ctx context.Context, req *examplepb.ExampleRequest) (*examplepb.ExampleResponse, error) {
	// Añade datos útiles a la traza
	if span := trace.SpanFromContext(ctx); span != nil {
		span.SetAttributes(attribute.String("example.id", req.GetId()))
		span.AddEvent("handling GetExample")
	}
	// Log JSON correlacionado (incluye trace_id/span_id)
	logger.Log(ctx).Info("GetExample called",
		slog.String("example_id", req.GetId()),
	)

	if strings.Contains(req.GetId(), "error") {
		logger.Log(ctx).ErrorContext(ctx, fmt.Sprintf("Internal server error simulated: id=%s", req.GetId()))
	}
	msg := fmt.Sprintf("Hola %s 👋", req.GetId())
	return &examplepb.ExampleResponse{
		Message:   msg,
		Timestamp: timestamppb.Now(),
	}, nil
}
