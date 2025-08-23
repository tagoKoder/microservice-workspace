package identity

import (
	"context"
	"time"

	"github.com/tagoKoder/gateway/pkg/grpcx"
	identitypb "github.com/tagoKoder/proto/genproto/go/identity"
	"google.golang.org/grpc"
)

type IdentityProvider struct {
	conn     *grpc.ClientConn
	identity identitypb.IdentityServiceClient
	// si mañana hay más services: users, roles, etc., los agregas aquí
}

func NewIdentityProvider(ctx context.Context, addr string) (*IdentityProvider, error) {
	conn, err := grpcx.Dial(ctx, addr, grpcx.DialOptions{
		Insecure: true,
		Timeout:  15 * time.Second,
	})
	if err != nil {
		return nil, err
	}
	return &IdentityProvider{
		conn:     conn,
		identity: identitypb.NewIdentityServiceClient(conn),
	}, nil
}

func (p *IdentityProvider) Close() error { return p.conn.Close() }

// Expones wrappers de alto nivel (contratos del gateway)
func (p *IdentityProvider) Identity() IdentityAPI {
	return &identityClient{pb: p.identity}
}
