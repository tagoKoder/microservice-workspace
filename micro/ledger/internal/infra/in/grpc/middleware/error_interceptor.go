package middleware

import (
	"context"
	"errors"
	"log"
	"strings"

	"github.com/google/uuid"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/metadata"
	"google.golang.org/grpc/status"
)

// NewUnaryErrorInterceptor:
// - genera/propaga x-correlation-id (trailer)
// - recovery ante panic
// - evita filtrar err.Error() en prod (convierte a mensajes seguros)
func NewUnaryErrorInterceptor(appEnv string) grpc.UnaryServerInterceptor {
	return func(ctx context.Context, req any, info *grpc.UnaryServerInfo, handler grpc.UnaryHandler) (resp any, err error) {
		cid := getOrCreateCorrelationID(ctx)
		_ = grpc.SetTrailer(ctx, metadata.Pairs("x-correlation-id", cid))

		defer func() {
			if r := recover(); r != nil {
				// NO loguees req (puede tener PII). Solo método + cid.
				log.Printf("PANIC cid=%s method=%s err=%v", cid, info.FullMethod, r)
				err = status.Error(codes.Internal, "internal")
				resp = nil
			}
		}()

		resp, err = handler(ctx, req)
		if err == nil {
			return resp, nil
		}

		// Si el handler devolvió un status gRPC, lo saneamos (según env + code)
		if st, ok := status.FromError(err); ok {
			return nil, sanitizeStatusError(st, appEnv, cid, info.FullMethod)
		}

		// Si NO es status error: gRPC lo convertiría a Unknown + err.Error() (filtra detalles).
		// Lo bloqueamos aquí.
		log.Printf("UNEXPECTED_ERROR cid=%s method=%s err=%v", cid, info.FullMethod, err)
		if isLocal(appEnv) {
			// en local puedes ver el detalle (opcional)
			return nil, status.Errorf(codes.Internal, "internal: %v", err)
		}
		return nil, status.Error(codes.Internal, "internal")
	}
}

func sanitizeStatusError(st *status.Status, appEnv, cid, method string) error {
	code := st.Code()
	msg := st.Message()

	// En local, normalmente quieres ver mensajes reales para debug.
	if isLocal(appEnv) {
		return st.Err()
	}

	// En no-local: solo permitimos mensajes “seguros” para ciertos códigos.
	// Para Unknown/Internal (y otros), devolvemos mensaje genérico.
	switch code {
	case codes.InvalidArgument,
		codes.Unauthenticated,
		codes.PermissionDenied,
		codes.NotFound,
		codes.AlreadyExists,
		codes.FailedPrecondition,
		codes.ResourceExhausted,
		codes.Canceled,
		codes.DeadlineExceeded:
		// estos suelen ser “esperados”. Igual sanitizamos para evitar que venga algo enorme.
		return status.Error(code, safeMsg(msg))

	default:
		// Para cualquier cosa inesperada, no filtramos msg.
		log.Printf("SANITIZED_ERROR cid=%s method=%s code=%s raw=%q", cid, method, code.String(), msg)
		return status.Error(codes.Internal, "internal")
	}
}

func safeMsg(s string) string {
	s = strings.TrimSpace(s)
	if s == "" {
		return "error"
	}
	// evita responses gigantes
	const max = 160
	if len(s) > max {
		s = s[:max] + "…"
	}
	// quita control chars
	s = strings.Map(func(r rune) rune {
		if r <= 0x1F || r == 0x7F {
			return -1
		}
		return r
	}, s)
	return s
}

func isLocal(env string) bool {
	env = strings.TrimSpace(strings.ToLower(env))
	return env == "" || env == "local" || env == "dev"
}

func getOrCreateCorrelationID(ctx context.Context) string {
	md, ok := metadata.FromIncomingContext(ctx)
	if ok {
		if vs := md.Get("x-correlation-id"); len(vs) > 0 && strings.TrimSpace(vs[0]) != "" {
			return strings.TrimSpace(vs[0])
		}
	}
	return uuid.NewString()
}

// (opcional) helper si luego quieres mapear errores por tipo
func is(err error, target error) bool { return errors.Is(err, target) }
