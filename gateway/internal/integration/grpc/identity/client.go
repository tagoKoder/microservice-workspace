package identity

import (
	"context"

	identitypb "github.com/tagoKoder/proto/genproto/go/identity"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
	"google.golang.org/grpc/metadata"
	"google.golang.org/protobuf/types/known/emptypb"
)

type Client struct {
	conn *grpc.ClientConn
	pb   identitypb.IdentityServiceClient
}

func New(addr string) (*Client, error) {
	conn, err := grpc.Dial(addr,
		grpc.WithTransportCredentials(insecure.NewCredentials()),
	)
	if err != nil {
		return nil, err
	}
	return &Client{
		conn: conn,
		pb:   identitypb.NewIdentityServiceClient(conn),
	}, nil
}

func (c *Client) Close() { _ = c.conn.Close() }

func (c *Client) WhoAmI(ctx context.Context, accessToken string) (*identitypb.WhoAmIResponse, error) {
	md := metadata.Pairs("authorization", "Bearer "+accessToken)
	return c.pb.WhoAmI(metadata.NewOutgoingContext(ctx, md), &emptypb.Empty{})
}

func (c *Client) Link(ctx context.Context, idToken string) (*identitypb.LinkResponse, error) {
	md := metadata.Pairs("authorization", "Bearer "+idToken)
	return c.pb.Link(metadata.NewOutgoingContext(ctx, md), &emptypb.Empty{})
}
