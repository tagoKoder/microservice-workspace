package db

import "context"

// Storage es la "conexión" abstracta a la BD (o datastore).
// Expone lo mínimo necesario: transacciones, health y cierre.
type Storage interface {
	// TxManager devuelve el orquestador de transacciones ACID locales.
	TxManager() TxManager

	// Ping para health checks (readiness/liveness).
	Ping(ctx context.Context) error

	// Close para liberar recursos/pool.
	Close() error
}
