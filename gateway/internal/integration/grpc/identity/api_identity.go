// internal/integration/grpc/identity/api.go
package identity

import (
	"context"

	identitypb "github.com/tagoKoder/proto/genproto/go/identity"
)

type IdentityAPI interface {
	WhoAmI(ctx context.Context, accessToken string) (*identitypb.WhoAmIResponse, error)
	Link(ctx context.Context, idToken string) (*identitypb.LinkResponse, error)
}
