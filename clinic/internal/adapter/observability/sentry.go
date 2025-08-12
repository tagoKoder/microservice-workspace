// internal/adapter/observability/sentry.go
package observability

import (
	"context"
	"time"

	"github.com/getsentry/sentry-go"
	"go.opentelemetry.io/otel/baggage"
	"go.opentelemetry.io/otel/trace"
)

func InitSentry(dsn, environment, release, service string) (flush func(), err error) {
	err = sentry.Init(sentry.ClientOptions{
		Dsn:              dsn,
		Environment:      environment,
		Release:          release,
		ServerName:       service,
		AttachStacktrace: true, // adjunta stack si el error no lo trae

		EnableTracing: false, // traces = Tempo; aquí solo errores
		// Puedes sanitizar/normalizar en BeforeSend si lo necesitas
		BeforeSend: func(e *sentry.Event, hint *sentry.EventHint) *sentry.Event {
			if e.Tags == nil {
				e.Tags = map[string]string{}
			}
			e.Tags["service"] = service
			e.Tags["environment"] = environment
			e.Tags["release"] = release
			return e
		},
	})
	return func() { sentry.Flush(2 * time.Second) }, err
}

// CaptureErr capta un error con el máximo contexto disponible de OTel/baggage.
func CaptureErr(ctx context.Context, err error, extras map[string]any) *sentry.EventID {
	if err == nil {
		return nil
	}
	h := sentry.CurrentHub().Clone()
	scope := h.Scope()
	// OTel trace/span → tags de Sentry para correlacionar con Tempo/Loki
	if sc := trace.SpanContextFromContext(ctx); sc.IsValid() {
		scope.SetTag("trace_id", sc.TraceID().String())
		scope.SetTag("span_id", sc.SpanID().String())
	}

	// Baggage → tags útiles de negocio
	b := baggage.FromContext(ctx)
	if m := b.Member("bc.user_id"); m.Key() != "" {
		scope.SetTag("user_id", m.Value())
	}
	if m := b.Member("bc.agency_id"); m.Key() != "" {
		scope.SetTag("agency_id", m.Value())
	}

	for k, v := range extras {
		scope.SetExtra(k, v)
	}
	eid := h.CaptureException(err)
	return eid
}
