package identity

import (
	"context"

	"google.golang.org/protobuf/types/known/emptypb"

	identitypb "github.com/tagoKoder/proto/genproto/go/identity"
)

type identityClient struct {
	pb identitypb.IdentityServiceClient
}

func (c *identityClient) WhoAmI(ctx context.Context, accessToken string) (*identitypb.WhoAmIResponse, error) {
	ctx = WithBearer(ctx, accessToken)
	return c.pb.WhoAmI(ctx, &emptypb.Empty{})
}

func (c *identityClient) Link(ctx context.Context, idToken string) (*identitypb.LinkResponse, error) {
	ctx = WithBearer(ctx, idToken)
	return c.pb.Link(ctx, &emptypb.Empty{})
}
