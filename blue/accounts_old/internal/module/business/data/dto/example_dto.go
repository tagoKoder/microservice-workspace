package dto

import (
	"strings"

	examplepb "github.com/tagoKoder/proto/genproto/go/example"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

func IsValid(r *examplepb.CreateBusinessRequest) error {
	name := strings.TrimSpace(r.GetName())
	govID := strings.TrimSpace(r.GetGovernmentId())
	if name == "" || govID == "" {
		return status.Error(codes.InvalidArgument, "name and government_id are required")
	}
	return nil
}
