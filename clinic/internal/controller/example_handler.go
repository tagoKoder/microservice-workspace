package controller

import (
	"context"
	"fmt"
	"log/slog"
	"strings"

	commonLog "github.com/tagoKoder/common-kit/pkg/logging"
	"go.opentelemetry.io/otel/attribute"
	"go.opentelemetry.io/otel/trace"

	examplepb "github.com/tagoKoder/proto/genproto/go/example"
	"google.golang.org/protobuf/types/known/timestamppb"
)

type ExampleController struct {
	examplepb.UnimplementedExampleServiceServer
}

func NewExampleController() *ExampleController { return &ExampleController{} }

func (h *ExampleController) GetExample(ctx context.Context, req *examplepb.ExampleRequest) (*examplepb.ExampleResponse, error) {
	// Añade datos útiles a la traza
	if span := trace.SpanFromContext(ctx); span != nil {
		span.SetAttributes(attribute.String("example.id", req.GetId()))
		span.AddEvent("handling GetExample")
	}
	// Log JSON correlacionado (incluye trace_id/span_id)
	commonLog.From(ctx).Info("GetExample called",
		slog.String("example_id", req.GetId()),
	)

	if strings.Contains(req.GetId(), "error") {
		commonLog.From(ctx).ErrorContext(ctx, fmt.Sprintf("Internal server error simulated: id=%s", req.GetId()))
	}
	msg := fmt.Sprintf("Hola %s 👋", req.GetId())
	return &examplepb.ExampleResponse{
		Message:   msg,
		Timestamp: timestamppb.Now(),
	}, nil
}
