package mapper

import (
	"github.com/tagoKoder/accounts/internal/module/business/data/model"
	examplepb "github.com/tagoKoder/proto/genproto/go/example"
)

func BusinessFromProtoToBusiness(in *examplepb.CreateBusinessRequest) *model.Business {
	return &model.Business{
		Name:         in.GetName(),
		GovernmentID: in.GetGovernmentId(),
	}
}
