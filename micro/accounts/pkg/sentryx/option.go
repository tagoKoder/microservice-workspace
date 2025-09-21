package sentryx

import (
	commonInterceptor "github.com/tagoKoder/common-kit/interceptor"
	"go.opentelemetry.io/contrib/instrumentation/google.golang.org/grpc/otelgrpc"
	"go.opentelemetry.io/otel"
	"google.golang.org/grpc"
)

// ServerOptions: trazas + métricas + propagación + Sentry
func ServerOptions() []grpc.ServerOption {
	return []grpc.ServerOption{
		grpc.StatsHandler(otelgrpc.NewServerHandler(
			otelgrpc.WithTracerProvider(otel.GetTracerProvider()),
			otelgrpc.WithMeterProvider(otel.GetMeterProvider()),
			otelgrpc.WithPropagators(otel.GetTextMapPropagator()),
		)),
		grpc.ChainUnaryInterceptor(commonInterceptor.UnaryServerSentry()),
		grpc.ChainStreamInterceptor(commonInterceptor.StreamServerSentry()),
	}
}
