//go:build integration
// +build integration

package integration

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/require"
	tc "github.com/testcontainers/testcontainers-go"
	"github.com/testcontainers/testcontainers-go/wait"

	ckpg "github.com/tagoKoder/common-kit/pkg/db/postgres"
	glogger "gorm.io/gorm/logger"

	"github.com/tagoKoder/accounts/internal/domain/model"
	"github.com/tagoKoder/accounts/internal/repository/uow"
	"github.com/tagoKoder/accounts/internal/service/impl"
)

func pgContainer(t *testing.T) (dsn string, terminate func()) {
	t.Helper()
	ctx := context.Background()
	req := tc.ContainerRequest{
		Image:        "postgres:16",
		Env:          map[string]string{"POSTGRES_PASSWORD": "postgres", "POSTGRES_DB": "clinic"},
		ExposedPorts: []string{"5432/tcp"},
		WaitingFor:   wait.ForLog("database system is ready to accept connections"),
	}
	c, err := tc.GenericContainer(ctx, tc.GenericContainerRequest{ContainerRequest: req, Started: true})
	require.NoError(t, err)
	host, _ := c.Host(ctx)
	port, _ := c.MappedPort(ctx, "5432/tcp")
	dsn = "host=" + host + " port=" + port.Port() + " user=postgres password=postgres dbname=clinic sslmode=disable TimeZone=UTC application_name=itest options='-c search_path=public'"
	return dsn, func() { _ = c.Terminate(ctx) }
}

func TestCreate_Integration(t *testing.T) {
	dsn, stop := pgContainer(t)
	defer stop()

	db, _, err := ckpg.OpenPostgres(ckpg.OpenOpts{
		DSN: dsn, LogLevel: glogger.Silent, Slow: 200 * time.Millisecond, PrepareStmt: false, UseOTel: false,
	})
	require.NoError(t, err)
	require.NoError(t, db.AutoMigrate(&model.Business{}))

	tx := uow.NewTxManager(db)
	qr := uow.NewQueryManager(db)
	svc := impl.NewBusinessService(tx, qr)

	id, err := svc.Create(context.Background(), impl.CreateBusinessInput{Name: "acme", GovernmentID: "RUC1"})
	require.NoError(t, err)
	got, err := svc.GetByID(context.Background(), id)
	require.NoError(t, err)
	require.Equal(t, "acme", got.Name)
	require.False(t, got.CreatedAt.IsZero())
}
