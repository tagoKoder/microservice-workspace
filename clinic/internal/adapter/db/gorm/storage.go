package gorm

import (
	"context"
	"database/sql"
	"time"

	"github.com/tagoKoder/clinic/internal/adapter/db"
	"gorm.io/gorm"
)

// storageGorm implementa db.Storage y oculta *gorm.DB.
type storageGorm struct {
	db        *gorm.DB
	sqlDB     *sql.DB
	txManager db.TxManager
}

// NewStorageGorm recibe un *gorm.DB ya abierto (ver connection.go) y aplica pool + migraciones.
// Devuelve un Storage independiente del motor concreto.
func NewStorageGorm(db *gorm.DB) (*storageGorm, error) {
	sqlDB, err := db.DB()
	if err != nil {
		return nil, err
	}
	// tuning de pool (puedes parametrizar)
	sqlDB.SetMaxIdleConns(10)
	sqlDB.SetMaxOpenConns(50)
	sqlDB.SetConnMaxLifetime(30 * time.Minute)

	s := &storageGorm{db: db, sqlDB: sqlDB}
	s.txManager = NewTxManager(db) // tu TxManager gorm
	return s, nil
}

func (s *storageGorm) TxManager() db.TxManager { return s.txManager }

func (s *storageGorm) Ping(ctx context.Context) error {
	return s.sqlDB.PingContext(ctx)
}

func (s *storageGorm) Close() error { return s.sqlDB.Close() }
