// internal/observability/metrics.go
package observability

import (
	"context"
	"os"
	"time"

	"go.opentelemetry.io/otel"
	sdkmetric "go.opentelemetry.io/otel/sdk/metric"
	"go.opentelemetry.io/otel/sdk/resource"
	semconv "go.opentelemetry.io/otel/semconv/v1.26.0"

	otlpmetric "go.opentelemetry.io/otel/exporters/otlp/otlpmetric/otlpmetricgrpc"
	"google.golang.org/grpc"
)

func SetupMetricsOTLP(ctx context.Context, serviceName, env, version, endpoint string) (func(context.Context) error, error) {
	if endpoint == "" {
		if e := os.Getenv("OTEL_EXPORTER_OTLP_ENDPOINT"); e != "" {
			endpoint = e
		} else {
			endpoint = "otel-collector:4317"
		}
	}

	exp, err := otlpmetric.New(ctx,
		otlpmetric.WithEndpoint(endpoint),
		otlpmetric.WithInsecure(),
		otlpmetric.WithDialOption(grpc.WithBlock()),
	)
	if err != nil {
		return nil, err
	}

	res, _ := resource.Merge(resource.Default(),
		resource.NewWithAttributes(
			semconv.SchemaURL,
			semconv.ServiceName(serviceName),
			semconv.ServiceVersion(version),
			semconv.DeploymentEnvironment(env),
		),
	)

	reader := sdkmetric.NewPeriodicReader(exp, sdkmetric.WithInterval(15*time.Second))
	mp := sdkmetric.NewMeterProvider(
		sdkmetric.WithReader(reader),
		sdkmetric.WithResource(res),
	)
	otel.SetMeterProvider(mp)
	return mp.Shutdown, nil
}
