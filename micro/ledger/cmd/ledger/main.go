package main

import (
	"log"
	"net"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/tagoKoder/ledger/internal/application/service"
	"github.com/tagoKoder/ledger/internal/infra/config"
	"github.com/tagoKoder/ledger/internal/infra/in/grpc/handler"
	"github.com/tagoKoder/ledger/internal/infra/out/audit"
	"github.com/tagoKoder/ledger/internal/infra/out/gateway"
	"github.com/tagoKoder/ledger/internal/infra/out/messaging"
	gormdb "github.com/tagoKoder/ledger/internal/infra/out/persistence/gorm"
	gormuow "github.com/tagoKoder/ledger/internal/infra/out/persistence/gorm/uow"

	ledgerpb "github.com/tagoKoder/ledger/proto/gen/ledgerpayments/v1"
	"google.golang.org/grpc"
)

func main() {
	cfg := config.Load()

	if cfg.DBWriteDSN == "" {
		log.Fatal("DB_DSN (or DB_WRITE_DSN) is required")
	}

	// DB
	writeDB, err := gormdb.New(cfg.DBWriteDSN)
	if err != nil {
		log.Fatalf("db write: %v", err)
	}

	readDB := writeDB
	if cfg.DBReadDSN != "" {
		readDB, err = gormdb.New(cfg.DBReadDSN)
		if err != nil {
			log.Fatalf("db read: %v", err)
		}
	}

	// UoW (infra -> implements application.UnitOfWork)
	uow := gormuow.NewGormUnitOfWork(readDB, writeDB)

	// Kafka publisher (EventPublisherPort)
	pub := messaging.NewKafkaPublisher(cfg.KafkaBrokers, cfg.KafkaClientID)

	// AuditPort
	auditPort := audit.NewKafkaAudit(pub, cfg.AuditTopic)

	// AccountsGatewayPort (REST internal)
	accountsGW := gateway.NewAccountsHTTPGateway(cfg.AccountsBaseURL, cfg.AccountsInternalTok)

	// Application services
	paySvc := service.NewPaymentService(uow, accountsGW, auditPort)
	ledSvc := service.NewLedgerAppService(uow, accountsGW, auditPort)

	// gRPC handlers
	payH := handler.NewPaymentsHandler(paySvc, paySvc)
	ledH := handler.NewLedgerHandler(ledSvc, ledSvc, ledSvc)

	// Outbox worker
	outboxWorker := messaging.NewOutboxWorker(uow, pub, cfg.OutboxBatchSize, cfg.OutboxPollInterval)
	outboxWorker.Start()

	// gRPC server
	lis, err := net.Listen("tcp", cfg.GrpcAddr)
	if err != nil {
		log.Fatalf("listen: %v", err)
	}

	grpcSrv := grpc.NewServer(
	// aquí puedes meter interceptors: auth, logging, rate-limit, etc.
	)

	ledgerpb.RegisterPaymentsServiceServer(grpcSrv, payH)
	ledgerpb.RegisterLedgerServiceServer(grpcSrv, ledH)

	// graceful shutdown
	go func() {
		log.Printf("ledger-payments gRPC listening on %s env=%s", cfg.GrpcAddr, cfg.AppEnv)
		if err := grpcSrv.Serve(lis); err != nil {
			log.Fatalf("grpc serve: %v", err)
		}
	}()

	waitForShutdown(func() {
		outboxWorker.Stop()
		stopped := make(chan struct{})
		go func() {
			grpcSrv.GracefulStop()
			close(stopped)
		}()
		select {
		case <-stopped:
		case <-time.After(5 * time.Second):
			grpcSrv.Stop()
		}
		_ = lis.Close()
		log.Println("shutdown complete")
	})
}

func waitForShutdown(fn func()) {
	ch := make(chan os.Signal, 1)
	signal.Notify(ch, syscall.SIGINT, syscall.SIGTERM)
	<-ch
	fn()
}
