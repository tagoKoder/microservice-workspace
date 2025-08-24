package main

import (
	"context"
	"log"
	"net/http"
	"os/signal"
	"syscall"
	"time"

	commonLog "github.com/tagoKoder/common-kit/pkg/logging"
	commonObs "github.com/tagoKoder/common-kit/pkg/observability"
	commonSlog "github.com/tagoKoder/common-kit/pkg/observability/slogx"
	"github.com/tagoKoder/gateway/internal/config"
	httpserver "github.com/tagoKoder/gateway/internal/entrypoint/http"
)

func main() {
	ctx, cancel := context.WithTimeout(context.Background(), 20*time.Second)
	defer cancel()
	// Config
	cfg, err := config.LoadConfig()
	if err != nil {
		log.Fatal(err)
	}

	// 1) Logging
	if err := commonLog.Init(commonLog.LogOptions{
		Level: cfg.LogLevel, FilePath: cfg.LogFilePath,
		Prefix: cfg.BaggagePrefix, MaxKeys: cfg.BaggageMaxKeys, MaxVal: cfg.BaggageMaxVal,
	}); err != nil {
		log.Fatal(err)
	}

	// 2) Sentry
	flush, _ := commonObs.Init(commonObs.SentryOptions{
		DSN: cfg.SentryDSN, Environment: cfg.Environment, Release: cfg.Version, Service: cfg.ServiceName,
		BaggagePrefix: cfg.BaggagePrefix, BaggageAllow: cfg.BaggageAllow, BaggageDeny: cfg.BaggageDeny,
		BaggageMaxKeys: cfg.BaggageMaxKeys, BaggageMaxVal: cfg.BaggageMaxVal,
		UserIDKey: cfg.BaggageUserIDKey, UserEmailKey: cfg.BaggageUserEmailKey,
		EnableTracing: true,
	})
	defer flush()

	// 3) OpenTelemetry (traces + metrics + propagators)
	stopAll, _ := commonObs.Start(ctx, commonObs.OtelOptions{
		ServiceName: cfg.ServiceName, Environment: cfg.Environment, Version: cfg.Version,
		Endpoint: cfg.OtelExporterOtlpEndpoint, BlockOnExport: true,
	})
	defer stopAll(context.Background())

	// Context que se cancela en SIGINT/SIGTERM
	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer stop()

	// ---- Providers (gRPC) que usarán las versiones (v1, v2, ...) ----
	provider, err := httpserver.NewClientProvider(ctx, &httpserver.ClientProviderAddress{
		Identity: cfg.IdentityServiceAddr,
	})
	if err != nil {
		commonLog.From(ctx).Error("failed to create client provider", commonSlog.MapToAttr(map[string]any{"error": err}))
	}
	// Si falla la creación del server, cerramos el provider.
	defer func() {
		if err != nil {
			_ = provider.Close()
		}
	}()

	// ---- Servidor HTTP (monta /api y las versiones) ----
	srv, err := httpserver.NewHttpServer(ctx, cfg, provider)
	if err != nil {
		_ = provider.Close()
		commonLog.From(ctx).Error("failed to create http server", commonSlog.MapToAttr(map[string]any{"error": err}))
	}

	commonLog.From(ctx).Info("starting http server", commonSlog.MapToAttr(map[string]any{"port": cfg.HttpPort}))

	// Run
	go func() {
		if err := srv.Start(); err != nil && err != http.ErrServerClosed {
			commonLog.From(ctx).Error("server stopped", commonSlog.MapToAttr(map[string]any{"error": err}))
		}
	}()

	// Espera señal y apaga con gracia
	<-ctx.Done()
	shctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	_ = srv.Shutdown(shctx)
}
