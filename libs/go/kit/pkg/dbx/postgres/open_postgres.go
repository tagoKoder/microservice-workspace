// commonkit/db/postgres/open_postgres.go
package postgres

import (
	"database/sql"
	"fmt"
	"strings"
	"time"

	"github.com/tagoKoder/common-kit/pkg/dbx/gormx"
	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/attribute"
	"gorm.io/driver/postgres"
	"gorm.io/gorm"
	glogger "gorm.io/gorm/logger"
	"gorm.io/gorm/schema"
	tracing "gorm.io/plugin/opentelemetry/tracing"
)

type Pool struct {
	MaxIdle, MaxOpen int
	MaxLifetime      time.Duration
}

type OpenOpts struct {
	DSN         string
	SearchPath  []string
	LogLevel    glogger.LogLevel
	Slow        time.Duration
	PrepareStmt bool
	Pool        Pool
	UseOTel     bool   // <-- activar plugin OTel
	DBName      string // nombre lógico para spans
}

func withSearchPath(dsn string, schemas []string) string {
	if len(schemas) == 0 {
		return dsn
	}
	qs := make([]string, 0, len(schemas))
	for _, s := range schemas {
		qs = append(qs, `"`+strings.ReplaceAll(s, `"`, `""`)+`"`)
	}
	opt := " options='-c search_path=" + strings.Join(qs, ",") + "'"
	if strings.Contains(dsn, " options=") {
		return dsn
	}
	return dsn + opt
}

func OpenPostgres(o OpenOpts) (*gorm.DB, *sql.DB, error) {
	cfg := &gorm.Config{
		Logger:         gormx.NewLogxAdapter(o.LogLevel, o.Slow),
		PrepareStmt:    o.PrepareStmt,
		NamingStrategy: schema.NamingStrategy{SingularTable: true}, // What it does is use singular table names, the default is plural table names
	}
	dsn := withSearchPath(o.DSN, o.SearchPath)

	db, err := gorm.Open(postgres.Open(dsn), cfg)
	if err != nil {
		return nil, nil, fmt.Errorf("open postgres: %w", err)
	}

	if o.UseOTel {
		// Usa el TracerProvider global configurado en observability.Start(...)
		_ = db.Use(tracing.NewPlugin(
			tracing.WithDBSystem("postgresql"),
			tracing.WithAttributes(
				attribute.String("db.name", o.DBName),
			), // aparece en spans como atributo
			tracing.WithTracerProvider(otel.GetTracerProvider()), // opcional
			// tracing.WithQueryFormatter(func(stmt string) string { return sanitize(stmt) }),
			// tracing.WithoutMetrics(), // si prefieres métricas solo desde postgres_exporter
		))
	}

	sqlDB, err := db.DB()
	if err != nil {
		return nil, nil, err
	}
	if o.Pool.MaxIdle > 0 {
		sqlDB.SetMaxIdleConns(o.Pool.MaxIdle)
	}
	if o.Pool.MaxOpen > 0 {
		sqlDB.SetMaxOpenConns(o.Pool.MaxOpen)
	}
	if o.Pool.MaxLifetime > 0 {
		sqlDB.SetConnMaxLifetime(o.Pool.MaxLifetime)
	}

	return db, sqlDB, nil
}
