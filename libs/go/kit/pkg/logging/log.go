// pkg/obs/logx/log.go
package logx

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

var base *slog.Logger
var levelVar slog.LevelVar

type LogOptions struct {
	Level    string // debug|info|warn|error
	FilePath string // optional
	// Allow/deny for baggage:
	Prefix  string              // e.g. "custom."
	Allow   map[string]struct{} // nil => any with prefix
	Deny    map[string]struct{}
	MaxKeys int // 0 => no limit
	MaxVal  int // 0 => no limit
}

var defaults = LogOptions{Prefix: "custom.", MaxKeys: 16, MaxVal: 500}

func Init(opts LogOptions) error {
	if opts.Level == "" {
		opts.Level = "info"
	}
	if opts.MaxKeys == 0 {
		opts.MaxKeys = defaults.MaxKeys
	}
	if opts.MaxVal == 0 {
		opts.MaxVal = defaults.MaxVal
	}
	if opts.Prefix == "" {
		opts.Prefix = defaults.Prefix
	}

	switch strings.ToLower(opts.Level) {
	case "debug":
		levelVar.Set(slog.LevelDebug)
	case "warn", "warning":
		levelVar.Set(slog.LevelWarn)
	case "error":
		levelVar.Set(slog.LevelError)
	default:
		levelVar.Set(slog.LevelInfo)
	}

	var w io.Writer = os.Stdout
	if opts.FilePath != "" {
		_ = os.MkdirAll(filepath.Dir(opts.FilePath), 0o755)
		f, err := os.OpenFile(opts.FilePath, os.O_CREATE|os.O_APPEND|os.O_WRONLY, 0o644)
		if err != nil {
			return err
		}
		w = io.MultiWriter(os.Stdout, f)
	}
	base = slog.New(slog.NewJSONHandler(w, &slog.HandlerOptions{Level: &levelVar, AddSource: true}))
	slog.SetDefault(base)
	baggageAllow = opts.Allow
	baggageDeny = opts.Deny
	baggagePrefix = opts.Prefix
	baggageMaxKeys = opts.MaxKeys
	baggageMaxVal = opts.MaxVal
	return nil
}

var (
	baggageAllow   map[string]struct{}
	baggageDeny    map[string]struct{}
	baggagePrefix  string
	baggageMaxKeys int
	baggageMaxVal  int
)

func From(ctx context.Context) *slog.Logger {
	if base == nil {
		_ = Init(defaults)
	}
	kvs := make([]any, 0, 16)

	if sc := trace.SpanContextFromContext(ctx); sc.IsValid() {
		kvs = append(kvs, "trace_id", sc.TraceID().String(), "span_id", sc.SpanID().String())
	}

	// baggage filtered
	b := baggage.FromContext(ctx)
	n := 0
	for _, m := range b.Members() {
		k := m.Key()
		if baggagePrefix != "" && !strings.HasPrefix(k, baggagePrefix) {
			continue
		}
		if baggageAllow != nil {
			if _, ok := baggageAllow[k]; !ok {
				continue
			}
		}
		if baggageDeny != nil {
			if _, ok := baggageDeny[k]; ok {
				continue
			}
		}
		v := m.Value()
		if baggageMaxVal > 0 && len(v) > baggageMaxVal {
			v = v[:baggageMaxVal] + "…"
		}
		kvs = append(kvs, strings.NewReplacer(".", "_", "-", "_").Replace(k), v)
		n++
		if baggageMaxKeys > 0 && n >= baggageMaxKeys {
			break
		}
	}

	if len(kvs) > 0 {
		return base.With(kvs...)
	}
	return base
}
