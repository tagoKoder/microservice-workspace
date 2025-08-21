package observability

import (
	"context"
	"os"
	"time"

	"go.opentelemetry.io/contrib/instrumentation/runtime"
	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/exporters/otlp/otlpmetric/otlpmetricgrpc"
	"go.opentelemetry.io/otel/exporters/otlp/otlptrace/otlptracegrpc"
	"go.opentelemetry.io/otel/propagation"
	sdkmetric "go.opentelemetry.io/otel/sdk/metric"
	"go.opentelemetry.io/otel/sdk/resource"
	sdktrace "go.opentelemetry.io/otel/sdk/trace"
	semconv "go.opentelemetry.io/otel/semconv/v1.26.0"
	"google.golang.org/grpc"
)

type OtelOptions struct {
	ServiceName   string
	Environment   string
	Version       string
	Endpoint      string        // otel-collector:4317
	MetricPeriod  time.Duration // 0 => 15s
	TraceRatio    float64       // 0..1, 0 => default SDK
	BlockOnExport bool          // true => WithBlock()
}

type Shutdown func(ctx context.Context) error

type StopAll func(ctx context.Context) error

func Start(ctx context.Context, cfg OtelOptions) (StopAll, error) {

	// --- Exporters ---
	traceOpts := []otlptracegrpc.Option{
		otlptracegrpc.WithEndpoint(cfg.Endpoint),
		otlptracegrpc.WithInsecure(),
	}
	metricOpts := []otlpmetricgrpc.Option{
		otlpmetricgrpc.WithEndpoint(cfg.Endpoint),
		otlpmetricgrpc.WithInsecure(),
	}
	if cfg.BlockOnExport {
		traceOpts = append(traceOpts, otlptracegrpc.WithDialOption(grpc.WithBlock()))
		metricOpts = append(metricOpts, otlpmetricgrpc.WithDialOption(grpc.WithBlock()))
	}
	texp, err := otlptracegrpc.New(ctx, traceOpts...)
	if err != nil {
		return nil, err
	}
	mexp, err := otlpmetricgrpc.New(ctx, metricOpts...)
	if err != nil {
		return nil, err
	}

	// --- Resource ---
	host, _ := os.Hostname()
	res, _ := resource.Merge(resource.Default(),
		resource.NewWithAttributes(
			semconv.SchemaURL,
			semconv.ServiceName(cfg.ServiceName),
			semconv.ServiceVersion(cfg.Version),
			semconv.DeploymentEnvironment(cfg.Environment),
			semconv.ServiceInstanceID(host),
		),
	)

	// --- Traces ---
	tpOpts := []sdktrace.TracerProviderOption{
		sdktrace.WithBatcher(texp),
		sdktrace.WithResource(res),
	}
	// sampler opcional
	if cfg.TraceRatio > 0 && cfg.TraceRatio < 1 {
		tpOpts = append(tpOpts,
			sdktrace.WithSampler(sdktrace.ParentBased(sdktrace.TraceIDRatioBased(cfg.TraceRatio))),
		)
	}
	tp := sdktrace.NewTracerProvider(tpOpts...)
	otel.SetTracerProvider(tp)

	// --- Metrics ---
	period := cfg.MetricPeriod
	if period == 0 {
		period = 15 * time.Second
	}
	reader := sdkmetric.NewPeriodicReader(mexp, sdkmetric.WithInterval(period))
	mp := sdkmetric.NewMeterProvider(
		sdkmetric.WithReader(reader),
		sdkmetric.WithResource(res),
	)
	otel.SetMeterProvider(mp)

	// --- Propagators ---
	otel.SetTextMapPropagator(propagation.NewCompositeTextMapPropagator(
		propagation.TraceContext{}, propagation.Baggage{},
	))

	// --- Runtime metrics ---
	_ = runtime.Start(
		runtime.WithMinimumReadMemStatsInterval(15*time.Second),
		runtime.WithMeterProvider(mp),
	)

	return func(ctx context.Context) error {
		// apaga primero métricas y luego trazas
		if err := mp.Shutdown(ctx); err != nil {
			return err
		}
		if err := tp.Shutdown(ctx); err != nil {
			return err
		}
		return nil
	}, nil
}
