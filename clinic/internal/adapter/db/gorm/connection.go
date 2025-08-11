package gorm

import (
	"database/sql"
	"fmt"
	"strings"
	"time"

	"log/slog"

	"gorm.io/driver/postgres"
	"gorm.io/gorm"
	glogger "gorm.io/gorm/logger"
	"gorm.io/gorm/schema"
)

// OpenPostgres: conexión mínima con Postgres usando GORM + slog.
// - dsn: "host=... user=... password=... dbname=... sslmode=disable TimeZone=UTC"
// - searchPath: si quieres fijar el search_path (p.ej. []string{"order","public"}). Puede ser nil.
// - logLevel: glogger.Warn recomendado en prod; glogger.Info para ver SQL.
// Devuelve *gorm.DB y *sql.DB (para health/close).
func OpenPostgres(dsn string, searchPath []string, logLevel glogger.LogLevel) (*gorm.DB, *sql.DB, error) {
	gcfg := &gorm.Config{
		Logger:      NewSlogGormLogger(slog.Default(), logLevel, 200*time.Millisecond),
		PrepareStmt: true,
		NamingStrategy: schema.NamingStrategy{
			SingularTable: true,
		},
		// Sin NamingStrategy "fancy": si necesitas esquema fijo, usa TableName() en tus modelos,
		// o configura search_path aquí abajo.
	}

	db, err := gorm.Open(postgres.Open(dsn), gcfg)
	if err != nil {
		return nil, nil, fmt.Errorf("open postgres: %w", err)
	}

	sqlDB, err := db.DB()
	if err != nil {
		return nil, nil, err
	}
	// Pool sensato (ajusta si hace falta)
	sqlDB.SetMaxIdleConns(10)
	sqlDB.SetMaxOpenConns(50)
	sqlDB.SetConnMaxLifetime(30 * time.Minute)

	// Opcional: setear search_path si lo pasaste
	if len(searchPath) > 0 {
		var quoted []string
		for _, s := range searchPath {
			quoted = append(quoted, `"`+strings.ReplaceAll(s, `"`, `""`)+`"`)
		}
		q := `SET search_path TO ` + strings.Join(quoted, ", ")
		if err := db.Exec(q).Error; err != nil {
			return nil, nil, fmt.Errorf("set search_path: %w", err)
		}
	}

	return db, sqlDB, nil
}
