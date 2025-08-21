package grpcx

import (
	"context"
	"net"
	"time"

	"google.golang.org/grpc/peer"
)

// PeerAddr devuelve ip:puerto del cliente si está disponible.
func PeerAddr(ctx context.Context) string {
	if p, ok := peer.FromContext(ctx); ok && p.Addr != net.Addr(nil) {
		return p.Addr.String()
	}
	return ""
}

// DeadlineMS devuelve ms restantes para el deadline; -1 si no hay deadline.
func DeadlineMS(ctx context.Context) int64 {
	if dl, ok := ctx.Deadline(); ok {
		return time.Until(dl).Milliseconds()
	}
	return -1
}
