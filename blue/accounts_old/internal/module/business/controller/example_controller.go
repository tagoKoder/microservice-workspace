package controller

import (
	"context"
	"fmt"
	"log/slog"
	"strings"

	"github.com/tagoKoder/accounts/internal/module/business/data/dto"
	"github.com/tagoKoder/accounts/internal/module/business/service"
	commonLog "github.com/tagoKoder/common-kit/pkg/logging"
	"go.opentelemetry.io/otel/attribute"
	"go.opentelemetry.io/otel/trace"

	examplepb "github.com/tagoKoder/proto/genproto/go/example"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
	"google.golang.org/protobuf/types/known/timestamppb"
)

type ExampleController struct {
	examplepb.UnimplementedExampleServiceServer
	biz *service.BusinessService
}

func NewExampleController(biz *service.BusinessService) *ExampleController {
	return &ExampleController{biz: biz}
}

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

func (h *ExampleController) CreateBusiness(ctx context.Context, req *examplepb.CreateBusinessRequest) (*examplepb.CreateBusinessResponse, error) {
	if err := dto.IsValid(req); err != nil {
		return nil, err
	}

	commonLog.From(ctx).Info("CreateBusiness called",
		slog.String("name", req.GetName()), slog.String("gov_id", req.GetGovernmentId()),
	)

	id, err := h.biz.Create(ctx, req)
	if err != nil {
		return nil, status.Errorf(codes.Internal, "create business: %v", err)
	}

	// Traemos la fila para completar timestamps (tu service ya tiene GetByID)
	b, err := h.biz.GetByID(ctx, id)
	if err != nil {
		return nil, status.Errorf(codes.Internal, "get created business: %v", err)
	}
	if b == nil {
		return nil, status.Error(codes.Internal, "created business not found")
	}

	resp := &examplepb.CreateBusinessResponse{
		Id:           b.ID,
		Name:         b.Name,
		GovernmentId: b.GovernmentID,
		CreatedAt:    timestamppb.New(b.CreatedAt),
		UpdatedAt:    timestamppb.New(b.UpdatedAt),
	}
	commonLog.From(ctx).Info("CreateBusiness ok", slog.Int64("id", b.ID))
	return resp, nil
}
