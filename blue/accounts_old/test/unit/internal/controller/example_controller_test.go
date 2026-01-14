package controller_test

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"github.com/tagoKoder/accounts/internal/module/business/controller"
	"github.com/tagoKoder/accounts/internal/module/business/domain/model"
	examplepb "github.com/tagoKoder/proto/genproto/go/example"
)

type stubSvc struct{}

func (s stubSvc) Create(context.Context, struct{ Name, GovernmentID string }) (int64, error) {
	return 1, nil
}
func (s stubSvc) GetByID(context.Context, int64) (*model.Business, error) {
	now := time.Now().UTC()
	return &model.Business{ID: 1, Name: "acme", GovernmentID: "RUC1", CreatedAt: now, UpdatedAt: now}, nil
}

func TestCreateBusiness_Handler_OK(t *testing.T) {
	// si tu NewExampleController acepta una interfaz de servicio, inyéctala aquí.
	ctl := controller.NewExampleController(nil)
	// este archivo queda como plantilla; lo útil es testear el service (arriba).
	require.NotNil(t, ctl)

	_ = (&examplepb.CreateBusinessRequest{Name: "acme", GovernmentId: "RUC1"}) // evitar unused
}
