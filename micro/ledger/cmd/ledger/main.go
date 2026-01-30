// micro\ledger\cmd\ledger\main.go
package main

import (
	"context"
	"log"
	"net"
	"os"
	"os/signal"
	"syscall"
	"time"

	awscfg "github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/eventbridge"
	"github.com/aws/aws-sdk-go-v2/service/verifiedpermissions"
	"github.com/tagoKoder/ledger/internal/application/service"
	"github.com/tagoKoder/ledger/internal/infra/config"
	"github.com/tagoKoder/ledger/internal/infra/in/grpc/handler"
	"github.com/tagoKoder/ledger/internal/infra/out/audit"
	"github.com/tagoKoder/ledger/internal/infra/out/gateway"
	"github.com/tagoKoder/ledger/internal/infra/out/messaging"
	gormdb "github.com/tagoKoder/ledger/internal/infra/out/persistence/gorm"
	gormuow "github.com/tagoKoder/ledger/internal/infra/out/persistence/gorm/uow"
	"github.com/tagoKoder/ledger/internal/infra/security/authz"
	"github.com/tagoKoder/ledger/internal/infra/security/avp"
	securitygrpc "github.com/tagoKoder/ledger/internal/infra/security/grpc"
	jwtvalidator "github.com/tagoKoder/ledger/internal/infra/security/jwt"

	ledgerpb "github.com/tagoKoder/ledger/internal/genproto/bank/ledgerpayments/v1"
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

	// AWS SDK (EventBridge  AVP)
	awsCfg, err := awscfg.LoadDefaultConfig(
		context.Background(),
		awscfg.WithRegion(cfg.AWSRegion),
	)
	if err != nil {
		log.Fatalf("aws cfg: %v", err)
	}

	// Kafka publisher (outbox events del dominio)
	eb := eventbridge.NewFromConfig(awsCfg)
	pub := messaging.NewEventBridgePublisher(eb, cfg.DomainEventBusName, "ledger-payments")

	vp := verifiedpermissions.NewFromConfig(awsCfg)

	// AuditPort: EventBridge best-effort (fallback a logs interno)
	auditPort := audit.NewEventBridgeAudit(
		eb,
		cfg.AuditEventBusName,
		cfg.AuditSource,
		cfg.AuditDetailType,
		cfg.ServiceName,
		cfg.AppEnv,
	)

	// AccountsGatewayPort (REST internal)
	accountsGW := gateway.NewAccountsHTTPGateway(cfg.AccountsBaseURL, cfg.AccountsInternalTok, cfg.InternalTokenHeader)

	// Application services
	paySvc := service.NewPaymentService(uow, accountsGW, auditPort)
	ledSvc := service.NewLedgerAppService(uow, accountsGW, auditPort)

	// gRPC handlers
	payH := handler.NewPaymentsHandler(paySvc, paySvc)
	ledH := handler.NewLedgerHandler(ledSvc, ledSvc, ledSvc)

	// Outbox worker
	outboxWorker := messaging.NewOutboxWorker(uow, pub, cfg.OutboxBatchSize, cfg.OutboxPollInterval)
	outboxWorker.Start()

	// Security: JWT validator  AVP  resolver  interceptor
	jv := jwtvalidator.New(cfg.JWTIssuer, cfg.JWTAudience, cfg.JWTJWKSURL)
	avpClient := avp.New(vp, cfg.AVPPolicyStoreID)
	actRes := authz.NewActionResolver()
	resRes := authz.NewResourceResolver(uow)
	authzItc := securitygrpc.NewAuthzInterceptor(
		jv,
		avpClient,
		actRes,
		resRes,
		auditPort,
		cfg.HashSaltIP,
		cfg.HashSaltUA,
	)

	// gRPC server
	lis, err := net.Listen("tcp", cfg.GrpcAddr)
	if err != nil {
		log.Fatalf("listen: %v", err)
	}

	grpcSrv := grpc.NewServer(
		grpc.ChainUnaryInterceptor(
			authzItc.Unary(),
		),
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
