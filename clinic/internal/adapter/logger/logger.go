package logger

import (
	"context"
	"io"
	"log/slog"
	"os"
	"path/filepath"
	"strings"

	"go.opentelemetry.io/otel/baggage"
	"go.opentelemetry.io/otel/trace"
)

var (
	base     *slog.Logger
	levelVar slog.LevelVar
)

// InitSlog configura el logger global JSON a stdout, con nivel desde string.
func InitSlog(level string, filePath string) error {
	switch strings.ToLower(level) {
	case "debug":
		levelVar.Set(slog.LevelDebug)
	case "warn", "warning":
		levelVar.Set(slog.LevelWarn)
	case "error":
		levelVar.Set(slog.LevelError)
	default:
		levelVar.Set(slog.LevelInfo)
	}

	// prepara writer
	var w io.Writer = os.Stdout
	if filePath != "" {
		if err := os.MkdirAll(filepath.Dir(filePath), 0o755); err != nil {
			return err
		}
		f, err := os.OpenFile(filePath, os.O_CREATE|os.O_APPEND|os.O_WRONLY, 0o644)
		if err != nil {
			return err
		}
		w = io.MultiWriter(os.Stdout, f)
	}

	h := slog.NewJSONHandler(w, &slog.HandlerOptions{
		Level:     &levelVar,
		AddSource: true, // agrega archivo:línea
	})

	base = slog.New(h)
	slog.SetDefault(base) // por si usas slog.Info(...) directo
	return nil
}

// Log devuelve un logger con trace/span y baggage del ctx.
func Log(ctx context.Context) *slog.Logger {
	if base == nil {
		InitSlog("info", "")
	}
	kvs := make([]any, 0, 8)

	// IDs de traza
	if sc := trace.SpanContextFromContext(ctx); sc.IsValid() {
		kvs = append(kvs,
			"trace_id", sc.TraceID().String(),
			"span_id", sc.SpanID().String(),
		)
	}

	// Baggage opcional (ej: inyectado por tus middlewares)
	b := baggage.FromContext(ctx)
	if m := b.Member("bc.user_id"); m.Key() != "" {
		kvs = append(kvs, "user_id", m.Value())
	}
	if m := b.Member("bc.agency_id"); m.Key() != "" {
		kvs = append(kvs, "agency_id", m.Value())
	}

	if len(kvs) > 0 {
		return base.With(kvs...)
	}
	return base
}
