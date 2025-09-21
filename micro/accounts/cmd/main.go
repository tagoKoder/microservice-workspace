// cmd/user-service/main.go
package main

import (
	"context"
	"log"
	"net"
	"os"
	"time"

	"github.com/tagoKoder/accounts/internal/config"
	"github.com/tagoKoder/accounts/internal/domain/model"
	"github.com/tagoKoder/accounts/internal/repository/uow"
	"github.com/tagoKoder/accounts/internal/service/impl"
	"github.com/tagoKoder/accounts/pkg/sentryx"

	"github.com/tagoKoder/accounts/internal/controller"
	ckpg "github.com/tagoKoder/common-kit/pkg/dbx/postgres"
	commonLog "github.com/tagoKoder/common-kit/pkg/logging"
	commonObs "github.com/tagoKoder/common-kit/pkg/observability"
	examplepb "github.com/tagoKoder/proto/genproto/go/example"
	"google.golang.org/grpc"
	"google.golang.org/grpc/reflection"
	glogger "gorm.io/gorm/logger"
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

	// 2) DB write/read (por ahora el mismo DSN; cuando tengas réplica cambias el read)
	writeDB, sqlDB, err := ckpg.OpenPostgres(ckpg.OpenOpts{
		DSN: cfg.DbDSN, // ej: host=localhost user=postgres password=postgres dbname=clinic sslmode=disable TimeZone=UTC application_name=clinic options='-c search_path=clinic,public'
		//SearchPath:  []string{"clinic", "public"},
		LogLevel:    glogger.Warn, // Info en dev
		Slow:        300 * time.Millisecond,
		PrepareStmt: true,
		UseOTel:     true,
		DBName:      "clinic",
	})
	if err != nil {
		log.Fatal("open db:", err)
	}
	defer sqlDB.Close()
	readDB := writeDB // cuando haya réplica: abre otra con cfg.PGReadDSN

	// 3) AutoMigrate opcional para local/dev (no lo uses en prod)
	if os.Getenv("DB_AUTO_MIGRATE") == "true" {
		if err := writeDB.AutoMigrate(&model.Business{}); err != nil {
			log.Fatal("automigrate:", err)
		}
	}

	// 4) Managers + service
	txMgr := uow.NewTxManager(writeDB)
	qMgr := uow.NewQueryManager(readDB)
	bizSvc := impl.NewBusinessService(txMgr, qMgr)

	// gRPC server instrumentado con StatsHandler (trazas + métricas gRPC)
	srv := grpc.NewServer(
		sentryx.ServerOptions()...,
	)

	// Reflection just for development
	if cfg.Environment == "dev" || cfg.Environment == "development" {
		reflection.Register(srv)
	}

	examplepb.RegisterExampleServiceServer(srv, controller.NewExampleController(bizSvc))

	lis, err := net.Listen("tcp", ":"+cfg.GrpcPort)
	if err != nil {
		log.Fatal(err)
	}

	log.Println("gRPC listening :" + cfg.GrpcPort)
	if err := srv.Serve(lis); err != nil {
		log.Fatal(err)
	}
}
