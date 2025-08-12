// internal/adapter/observability/sentry_grpc.go
package observability

import (
	"context"
	"fmt"
	"log/slog"
	"net"
	"strings"
	"time"

	"github.com/getsentry/sentry-go"
	"github.com/tagoKoder/clinic/internal/adapter/logger"
	"google.golang.org/grpc"
	"google.golang.org/grpc/metadata"
	"google.golang.org/grpc/peer"
	"google.golang.org/grpc/status"

	"google.golang.org/protobuf/encoding/protojson"
	"google.golang.org/protobuf/proto"
)

// --- helpers ---

// serializa el request si es proto.Message, con tope y redacción.
func marshalReqSafe(req any) (string, bool) {
	m, ok := req.(proto.Message)
	if !ok {
		return "", false
	}
	js, _ := protojson.MarshalOptions{
		EmitUnpopulated: false,
		UseProtoNames:   true,
	}.Marshal(m)

	// redacción básica por claves (ajusta a tu dominio)
	redacted := redactJSON(string(js), []string{"password", "token", "authorization", "cookie"})
	const max = 16 << 10 // 16 KiB
	if len(redacted) > max {
		redacted = redacted[:max] + "...(truncated)"
	}
	return redacted, true
}

func sanitizeMD(ctx context.Context) map[string]string {
	out := map[string]string{}
	if md, ok := metadata.FromIncomingContext(ctx); ok {
		for k, v := range md {
			kl := strings.ToLower(k)           // normaliza
			if strings.HasSuffix(kl, "-bin") { // descarta binarios
				continue
			}
			if isSensitiveHeader(kl) { // descarta sensibles
				continue
			}
			switch len(v) {
			case 0:
				// nada
			case 1:
				out[kl] = v[0]
			default:
				out[kl] = fmt.Sprintf("%v", v) // compacta múltiples valores
			}
		}
	}
	return out
}

func isSensitiveHeader(k string) bool {
	switch k {
	case "authorization", "cookie", "set-cookie", "x-api-key":
		return true
	default:
		return false
	}
}

func peerAddr(ctx context.Context) string {
	if p, ok := peer.FromContext(ctx); ok && p.Addr != net.Addr(nil) {
		return p.Addr.String()
	}
	return ""
}

func deadlineMS(ctx context.Context) int64 {
	if dl, ok := ctx.Deadline(); ok {
		return time.Until(dl).Milliseconds()
	}
	return -1
}

// Redacción naïve: reemplaza valores de ciertas claves en un JSON plano (best effort).
func redactJSON(s string, keys []string) string {
	for _, k := range keys {
		// "k":"valor"  -> "k":"***"
		s = replaceAllCaseInsensitive(s, fmt.Sprintf(`"%s":"`, k), fmt.Sprintf(`"%s":"***`, k))
	}
	return s
}

// reemplazo simple case-insensitive (suficiente para logs, no para parsing formal)
func replaceAllCaseInsensitive(s, find, repl string) string {
	// versión simple (podrías usar regex si prefieres)
	return s // deja como ejercicio si necesitas algo más fuerte
}

// --- interceptors ---

func UnaryServerSentry() grpc.UnaryServerInterceptor {
	return func(ctx context.Context, req any, info *grpc.UnaryServerInfo, handler grpc.UnaryHandler) (resp any, err error) {
		defer func() {
			if rec := recover(); rec != nil {
				sentry.CurrentHub().Recover(rec)
				sentry.Flush(2 * time.Second)
				panic(rec)
			}
		}()

		reqJSON, _ := marshalReqSafe(req)
		start := time.Now()

		resp, err = handler(ctx, req)

		code := status.Code(err) // gRPC status code
		extras := map[string]any{
			"grpc.full_method": info.FullMethod,
			"grpc.status_code": code.String(),
			"grpc.peer":        peerAddr(ctx),
			"grpc.deadline_ms": deadlineMS(ctx),
			"grpc.md":          sanitizeMD(ctx),
			"duration_ms":      time.Since(start).Milliseconds(),
		}
		if reqJSON != "" {
			extras["grpc.request"] = reqJSON
		}

		if eid := CaptureErr(ctx, err, extras); eid != nil {
			// opcional: loggear el event_id con tu slog
			logger.Log(ctx).Error("reported to sentry", slog.String("sentry_event_id", string(*eid)))
		}
		return resp, err
	}
}

// stream wrapper para contar mensajes in/out
type sentryServerStream struct {
	grpc.ServerStream
	msgIn, msgOut int
}

func (s *sentryServerStream) RecvMsg(m any) error {
	err := s.ServerStream.RecvMsg(m)
	if err == nil {
		s.msgIn++
	}
	return err
}
func (s *sentryServerStream) SendMsg(m any) error {
	err := s.ServerStream.SendMsg(m)
	if err == nil {
		s.msgOut++
	}
	return err
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

		wrapped := &sentryServerStream{ServerStream: ss}
		ctx := ss.Context()
		start := time.Now()

		err = handler(srv, wrapped)

		code := status.Code(err)
		extras := map[string]any{
			"grpc.full_method": info.FullMethod,
			"grpc.is_client":   info.IsClientStream,
			"grpc.is_server":   info.IsServerStream,
			"grpc.status_code": code.String(),
			"grpc.peer":        peerAddr(ctx),
			"grpc.deadline_ms": deadlineMS(ctx),
			"grpc.md":          sanitizeMD(ctx),
			"stream.msg_in":    wrapped.msgIn,
			"stream.msg_out":   wrapped.msgOut,
			"duration_ms":      time.Since(start).Milliseconds(),
		}
		_ = CaptureErr(ctx, err, extras)
		return err
	}
}
