package handler

import (
	"context"

	in "github.com/tagoKoder/ledger/internal/domain/port/in"
	ledgerpb "github.com/tagoKoder/ledger/internal/genproto/bank/ledgerpayments/v1"
	"github.com/tagoKoder/ledger/internal/infra/in/grpc/mapper"
)

type PaymentsHandler struct {
	ledgerpb.UnimplementedPaymentsServiceServer
	post in.PostPaymentUseCase
	get  in.GetPaymentUseCase
}

func NewPaymentsHandler(post in.PostPaymentUseCase, get in.GetPaymentUseCase) *PaymentsHandler {
	return &PaymentsHandler{post: post, get: get}
}

func (h *PaymentsHandler) PostPayment(ctx context.Context, req *ledgerpb.PostPaymentRequest) (*ledgerpb.PostPaymentResponse, error) {
	cmd, err := mapper.ToPostPaymentCommand(req)
	if err != nil {
		return nil, err
	}

	res, err := h.post.PostPayment(ctx, cmd)
	if err != nil {
		return nil, err
	}

	return mapper.ToPostPaymentResponse(&res), nil
}

func (h *PaymentsHandler) GetPayment(ctx context.Context, req *ledgerpb.GetPaymentRequest) (*ledgerpb.GetPaymentResponse, error) {
	id, err := mapper.ToGetPaymentID(req)
	if err != nil {
		return nil, err
	}

	res, err := h.get.GetPayment(ctx, id)
	if err != nil {
		return nil, err
	}

	return mapper.ToGetPaymentResponse(&res)
}
