// internal/integration/grpc/grpcx/dialer.go
package grpcx

import (
	"context"
	"time"

	"google.golang.org/grpc"
)

type DialOptions struct {
	Insecure bool          // true por ahora; luego TLS
	Timeout  time.Duration // opcional; 0 => sin WithBlock
}

// Dial crea la conexión con opciones razonables por defecto.
func Dial(ctx context.Context, addr string, opts DialOptions) (*grpc.ClientConn, error) {
	dialOpts := ClientDialOptions()

	if opts.Timeout > 0 {
		cctx, cancel := context.WithTimeout(ctx, opts.Timeout)
		defer cancel()
		return grpc.DialContext(cctx, addr, dialOpts...)
	}
	return grpc.DialContext(ctx, addr, dialOpts...)
}
