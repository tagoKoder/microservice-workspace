package httpx

import (
	"net/http"

	sentryhttp "github.com/getsentry/sentry-go/http"
	otelhttp "go.opentelemetry.io/contrib/instrumentation/net/http/otelhttp"
	"go.opentelemetry.io/otel"
)

type HttpxWrapperOptions struct {
	Operation       string
	WaitForDelivery bool
}

func Wrap(h http.Handler, opt HttpxWrapperOptions) http.Handler {
	if opt.Operation == "" {
		opt.Operation = "http"
	}
	oh := otelhttp.NewHandler(h, opt.Operation,
		otelhttp.WithTracerProvider(otel.GetTracerProvider()),
		otelhttp.WithMeterProvider(otel.GetMeterProvider()),
		otelhttp.WithPropagators(otel.GetTextMapPropagator()),
	)
	sh := sentryhttp.New(sentryhttp.Options{
		Repanic:         true,
		WaitForDelivery: opt.WaitForDelivery,
	})
	return sh.Handle(oh)
}
