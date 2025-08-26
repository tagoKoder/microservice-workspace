// cmd/user-service/main.go
package main

import (
	"context"
	"log"
	"net"
	"time"

	"github.com/tagoKoder/clinic/internal/config"
	"github.com/tagoKoder/clinic/pkg/sentryx"

	"github.com/tagoKoder/clinic/internal/controller"
	commonLog "github.com/tagoKoder/common-kit/pkg/logging"
	commonObs "github.com/tagoKoder/common-kit/pkg/observability"
	examplepb "github.com/tagoKoder/proto/genproto/go/example"
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

	// 1) Logging
	_ = commonLog.Init(commonLog.LogOptions{
		Level: cfg.LogLevel, FilePath: cfg.LogFilePath,
		Prefix: cfg.BaggagePrefix, MaxKeys: cfg.BaggageMaxKeys, MaxVal: cfg.BaggageMaxVal,
	})

	// 2) Sentry
	flush, _ := commonObs.Init(commonObs.SentryOptions{
		DSN: cfg.SentryDSN, Environment: cfg.Environment, Release: cfg.Version, Service: cfg.ServiceName,
		BaggagePrefix: cfg.BaggagePrefix, BaggageAllow: cfg.BaggageAllow, BaggageDeny: cfg.BaggageDeny,
		BaggageMaxKeys: cfg.BaggageMaxKeys, BaggageMaxVal: cfg.BaggageMaxVal, UserIDKey: cfg.BaggageUserIDKey, UserEmailKey: cfg.BaggageUserEmailKey,
		EnableTracing: true,
	})
	defer flush()

	// 3) OTel (traces + metrics + propagators)
	stopAll, _ := commonObs.Start(ctx, commonObs.OtelOptions{
		ServiceName: cfg.ServiceName, Environment: cfg.Environment, Version: cfg.Version, Endpoint: cfg.OtelExporterOtlpEndpoint, BlockOnExport: true,
	})
	defer stopAll(context.Background())

	// gRPC server instrumentado con StatsHandler (trazas + métricas gRPC)
	srv := grpc.NewServer(
		sentryx.ServerOptions()...,
	)

	// Reflection just for development
	if cfg.Environment == "dev" || cfg.Environment == "development" {
		reflection.Register(srv)
	}

	examplepb.RegisterExampleServiceServer(srv, controller.NewExampleController())

	lis, err := net.Listen("tcp", ":"+cfg.GrpcPort)
	if err != nil {
		log.Fatal(err)
	}

	log.Println("gRPC listening :" + cfg.GrpcPort)
	if err := srv.Serve(lis); err != nil {
		log.Fatal(err)
	}
}
