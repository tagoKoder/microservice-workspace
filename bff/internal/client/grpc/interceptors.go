package grpc

import (
	"context"
	"strings"

	"github.com/tagoKoder/bff/internal/security"
	"google.golang.org/grpc"
	"google.golang.org/grpc/metadata"
)

func OutgoingHeadersUnaryInterceptor() grpc.UnaryClientInterceptor {
	return func(ctx context.Context, method string, req, reply any, cc *grpc.ClientConn, invoker grpc.UnaryInvoker, opts ...grpc.CallOption) error {
		md, _ := metadata.FromOutgoingContext(ctx)
		md = md.Copy()

		if cid := security.CorrelationID(ctx); cid != "" {
			md.Set("x-correlation-id", cid)
		}

		if tok := security.AccessToken(ctx); tok != "" {
			// guard: si accidentalmente viene con "Bearer "
			if strings.HasPrefix(strings.ToLower(tok), "bearer ") {
				md.Set("authorization", tok)
			} else {
				md.Set("authorization", "Bearer "+tok)
			}
		}

		ctx = metadata.NewOutgoingContext(ctx, md)
		return invoker(ctx, method, req, reply, cc, opts...)
	}
}

func OutgoingHeadersStreamInterceptor() grpc.StreamClientInterceptor {
	return func(ctx context.Context, desc *grpc.StreamDesc, cc *grpc.ClientConn, method string, streamer grpc.Streamer, opts ...grpc.CallOption) (grpc.ClientStream, error) {
		md, _ := metadata.FromOutgoingContext(ctx)
		md = md.Copy()

		if cid := security.CorrelationID(ctx); cid != "" {
			md.Set("x-correlation-id", cid)
		}

		if tok := security.AccessToken(ctx); tok != "" {
			if strings.HasPrefix(strings.ToLower(tok), "bearer ") {
				md.Set("authorization", tok)
			} else {
				md.Set("authorization", "Bearer "+tok)
			}
		}

		ctx = metadata.NewOutgoingContext(ctx, md)
		return streamer(ctx, desc, cc, method, opts...)
	}
}
