// cmd/user-service/main.go
package main

import (
	"context"
	"log"
	"net"
	"time"

	"github.com/tagoKoder/clinic/internal/adapter/logger"
	"github.com/tagoKoder/clinic/internal/adapter/observability"
	"github.com/tagoKoder/clinic/internal/config"

	"github.com/tagoKoder/clinic/internal/handler"
	examplepb "github.com/tagoKoder/proto/genproto/go/example"
	otelgrpc "go.opentelemetry.io/contrib/instrumentation/google.golang.org/grpc/otelgrpc"
	"go.opentelemetry.io/otel"
	"google.golang.org/grpc"
	"google.golang.org/grpc/reflection"
)

func main() {
	ctx, cancel := context.WithTimeout(context.Background(), 20*time.Second)
	defer cancel()
	cfg, err := config.LoadConfig()

	if err != nil {
		log.Println("Error loading config:", err)
	}

	err = logger.InitSlog(cfg.LogLevel, cfg.LogFilePath) // "debug" | "info" | "warn" | "error"
	if err != nil {
		log.Fatal(err)
	}

	if cfg.SentryDNS != "" {
		flush, err := observability.InitSentry(cfg.SentryDNS, cfg.Environment, cfg.Version, cfg.ServiceName)
		if err != nil {
			log.Fatal(err)
		}
		defer flush()
	}

	// Traces -> OTLP (Collector)
	traceShutdown, err := observability.SetupTracer(ctx,
		cfg.ServiceName,
		cfg.Environment,
		cfg.Version,
		cfg.OtelExporterOtlpEndpoint,
	)
	if err != nil {
		log.Fatal(err)
	}
	defer traceShutdown(context.Background())

	// Metrics -> OTLP (Collector) **NO /metrics en el servicio**
	metricsShutdown, err := observability.SetupMetricsOTLP(ctx,
		cfg.ServiceName,
		cfg.Environment,
		cfg.Version,
		cfg.OtelExporterOtlpEndpoint,
	)
	if err != nil {
		log.Fatal(err)
	}
	defer metricsShutdown(context.Background())

	// gRPC server instrumentado con StatsHandler (trazas + métricas gRPC)
	srv := grpc.NewServer(
		grpc.StatsHandler(otelgrpc.NewServerHandler(
			otelgrpc.WithTracerProvider(otel.GetTracerProvider()),
			otelgrpc.WithMeterProvider(otel.GetMeterProvider()),
			otelgrpc.WithPropagators(otel.GetTextMapPropagator()),
		)),
		grpc.ChainUnaryInterceptor(
			observability.UnaryServerSentry(),
		),
		grpc.ChainStreamInterceptor(
			observability.StreamServerSentry(),
		),
	)

	// Reflection (solo dev recomendable)
	reflection.Register(srv) // <-- NUEVO

	// TODO: registra tus servicios aquí
	examplepb.RegisterExampleServiceServer(srv, handler.NewExampleHandler())

	lis, err := net.Listen("tcp", ":"+cfg.GrpcPort)
	if err != nil {
		log.Fatal(err)
	}

	log.Println("gRPC listening :" + cfg.GrpcPort)
	if err := srv.Serve(lis); err != nil {
		log.Fatal(err)
	}
}
