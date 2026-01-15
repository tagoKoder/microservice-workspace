package identity

import (
	"context"

	"google.golang.org/grpc/metadata"
)

func WithBearer(ctx context.Context, token string) context.Context {
	return metadata.AppendToOutgoingContext(ctx, "authorization", "Bearer "+token)
}
