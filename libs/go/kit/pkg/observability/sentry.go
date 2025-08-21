package observability

import (
	"context"
	"strings"
	"time"

	"github.com/getsentry/sentry-go"
	"github.com/tagoKoder/common-kit/pkg/observability/baggagex"
	"github.com/tagoKoder/common-kit/pkg/observability/sysx"
	"go.opentelemetry.io/otel/baggage"
	"go.opentelemetry.io/otel/trace"
	"google.golang.org/grpc/peer"
)

type SentryOptions struct {
	DSN, Environment, Release, Service string
	// --- Baggage filtering
	BaggagePrefix  string              // ej: "custom."
	BaggageAllow   map[string]struct{} // nil => any with prefix
	BaggageDeny    map[string]struct{} // keys to exclude
	BaggageMaxKeys int                 // 0 => no limit
	BaggageMaxVal  int                 // 0 => no limit
	// --- Optional Promotions to Sentry User
	UserIDKey     string // ej: "custom.user_id"
	UserEmailKey  string // ej: "custom.user_email"
	EnableTracing bool   // habilitar trazas con Tempo

}

var sopts SentryOptions

func Init(o SentryOptions) (flush func(), err error) {
	// defaults suaves
	if o.BaggageMaxKeys == 0 {
		o.BaggageMaxKeys = 16
	}
	if o.BaggageMaxVal == 0 {
		o.BaggageMaxVal = 256
	}
	if o.BaggagePrefix == "" {
		o.BaggagePrefix = "bc."
	}
	if o.Service == "" {
		o.Service = sysx.GuessService()
	}

	sopts = o

	err = sentry.Init(sentry.ClientOptions{
		Dsn:              o.DSN,
		Environment:      o.Environment,
		Release:          o.Release,
		ServerName:       o.Service,
		AttachStacktrace: true,
		EnableTracing:    o.EnableTracing,
		BeforeSend: func(e *sentry.Event, hint *sentry.EventHint) *sentry.Event {
			if e.Tags == nil {
				e.Tags = map[string]string{}
			}
			e.Tags["service"] = o.Service
			e.Tags["environment"] = o.Environment
			e.Tags["release"] = o.Release
			return e
		},
	})
	return func() { sentry.Flush(2 * time.Second) }, err
}

// CaptureErr capta err y añade: trace/span, baggage filtrado como tags, extras, y opcionalmente user.
func CaptureErr(ctx context.Context, err error, extras map[string]any) *sentry.EventID {
	if err == nil {
		return nil
	}

	h := sentry.CurrentHub().Clone()
	scope := h.Scope()

	// Correlación con OTel
	if sc := trace.SpanContextFromContext(ctx); sc.IsValid() {
		scope.SetTag("trace_id", sc.TraceID().String())
		scope.SetTag("span_id", sc.SpanID().String())
	}

	// Tags desde baggage, con el mismo esquema que logx
	bagTags := baggageTags(ctx,
		sopts.BaggagePrefix,
		sopts.BaggageAllow,
		sopts.BaggageDeny,
		sopts.BaggageMaxKeys,
		sopts.BaggageMaxVal,
	)
	for k, v := range bagTags {
		scope.SetTag(k, v)
	}

	// Promoción opcional a Sentry User
	user := sentry.User{}
	if sopts.UserIDKey != "" {
		if v, ok := baggagex.Value(ctx, sopts.UserIDKey); ok {
			user.ID = v
		}
	}

	if sopts.UserEmailKey != "" {
		if v, ok := baggagex.Value(ctx, sopts.UserEmailKey); ok {
			user.Email = v
		}
	}
	// IP del peer si no es HTTP (best-effort)
	if p, ok := peer.FromContext(ctx); ok && p != nil && p.Addr != nil {
		user.IPAddress = p.Addr.String()
	}
	if user.ID != "" || user.Email != "" {
		scope.SetUser(user)
	}

	// Extras arbitrarios del interceptor / capa superior
	for k, v := range extras {
		scope.SetExtra(k, v)
	}

	id := h.CaptureException(err)
	return id
}

func baggageTags(ctx context.Context,
	prefix string,
	allow, deny map[string]struct{},
	maxKeys, maxVal int,
) map[string]string {
	b := baggage.FromContext(ctx)
	if b.Len() == 0 {
		return nil
	}

	out := make(map[string]string, b.Len())
	n := 0
	r := strings.NewReplacer(".", "_", "-", "_")
	for _, m := range b.Members() {
		k := m.Key()
		if prefix != "" && !strings.HasPrefix(k, prefix) {
			continue
		}
		if allow != nil {
			if _, ok := allow[k]; !ok {
				continue
			}
		}
		if deny != nil {
			if _, ok := deny[k]; ok {
				continue
			}
		}
		v := m.Value()
		if maxVal > 0 && len(v) > maxVal {
			v = v[:maxVal] + "…"
		}
		out[r.Replace(k)] = v
		n++
		if maxKeys > 0 && n >= maxKeys {
			break
		}
	}
	return out
}
