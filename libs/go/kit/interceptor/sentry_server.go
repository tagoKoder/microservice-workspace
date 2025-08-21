package interceptor

import (
	"context"
	"time"

	"github.com/getsentry/sentry-go"
	"github.com/tagoKoder/common-kit/pkg/observability"
	"github.com/tagoKoder/common-kit/pkg/observability/grpcx"
	"google.golang.org/grpc"
	"google.golang.org/grpc/status"
)

func UnaryServerSentry() grpc.UnaryServerInterceptor {
	return func(ctx context.Context, req any, info *grpc.UnaryServerInfo, handler grpc.UnaryHandler) (any, error) {
		defer func() {
			if rec := recover(); rec != nil {
				sentry.CurrentHub().Recover(rec)
				panic(rec)
			}
		}()
		start := time.Now()
		resp, err := handler(ctx, req)
		extras := map[string]any{
			"grpc.full_method": info.FullMethod,
			"grpc.status_code": status.Code(err).String(),
			"grpc.peer":        grpcx.PeerAddr(ctx),
			"grpc.md":          grpcx.SanitizeMetadata(ctx),
			"duration_ms":      time.Since(start).Milliseconds(),
		}
		_ = observability.CaptureErr(ctx, err, extras)
		return resp, err
	}
}

func StreamServerSentry() grpc.StreamServerInterceptor {
	return func(srv any, ss grpc.ServerStream, info *grpc.StreamServerInfo, handler grpc.StreamHandler) (err error) {
		defer func() {
			if rec := recover(); rec != nil {
				sentry.CurrentHub().Recover(rec)
				sentry.Flush(2 * time.Second)
				panic(rec)
			}
		}()

		wrapped := &observability.SentryServerStream{ServerStream: ss}
		ctx := ss.Context()
		start := time.Now()

		err = handler(srv, wrapped)

		code := status.Code(err)
		extras := map[string]any{
			"grpc.full_method": info.FullMethod,
			"grpc.is_client":   info.IsClientStream,
			"grpc.is_server":   info.IsServerStream,
			"grpc.status_code": code.String(),
			"grpc.peer":        grpcx.PeerAddr(ctx),
			"grpc.deadline_ms": grpcx.DeadlineMS(ctx),
			"grpc.md":          grpcx.SanitizeMetadata(ctx),
			"stream.msg_in":    wrapped.MsgIn,
			"stream.msg_out":   wrapped.MsgOut,
			"duration_ms":      time.Since(start).Milliseconds(),
		}
		_ = observability.CaptureErr(ctx, err, extras)
		return err
	}
}
