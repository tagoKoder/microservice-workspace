package http

import (
	"context"

	identitygrpc "github.com/tagoKoder/gateway/internal/integration/grpc/identity"
)

// Providers son las interfaces que consumen las versiones v1, v2, etc.
type ClientProvider struct {
	identityProv *identitygrpc.IdentityProvider
}

func NewClientProvider(ctx context.Context, addr *ClientProviderAddress) (*ClientProvider, error) {
	cs, err := identitygrpc.NewIdentityProvider(ctx, addr.Identity)
	if err != nil {
		return nil, err
	}
	return &ClientProvider{identityProv: cs}, nil
}

func (p *ClientProvider) Close() error {
	if p.identityProv != nil {
		return p.identityProv.Close()
	}
	return nil
}

func (p *ClientProvider) IdentityProvider() *identitygrpc.IdentityProvider {
	return p.identityProv
}
