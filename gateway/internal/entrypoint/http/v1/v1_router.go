// internal/entrypoint/http/v1/router.go
package v1

import (
	"github.com/gorilla/mux"

	mw "github.com/tagoKoder/gateway/internal/entrypoint/http/middleware"

	// sistema: administrator-web
	adminweb "github.com/tagoKoder/gateway/internal/entrypoint/http/v1/administrator-web"
	enduserapp "github.com/tagoKoder/gateway/internal/entrypoint/http/v1/end-user-app"
	"github.com/tagoKoder/gateway/internal/entrypoint/http/v1/webhook"
	idcli "github.com/tagoKoder/gateway/internal/integration/grpc/identity"
)

type V1RouterDeps struct {
	Auth              mw.AccessTokenVerifier
	ID                idcli.IdentityAPI
	AuthentikApiKey   string
	HeaderIDTokenName string
}

type V1Router struct {
	deps V1RouterDeps
}

func NewRouter(d V1RouterDeps) *V1Router {
	return &V1Router{deps: d}
}

// Register monta TODO lo de v1 bajo /v1 (cada sistema bajo su propio subpath).
func (r *V1Router) Register(api *mux.Router) error {
	v1 := api.PathPrefix("/v1").Subrouter()

	// Sistema: administrator-web
	admin := adminweb.NewAdminWebRouter(&adminweb.AdminWebRouterDeps{
		IdentityClient:    r.deps.ID,
		Auth:              r.deps.Auth,
		HeaderIDTokenName: r.deps.HeaderIDTokenName,
	})
	if err := admin.Register(v1); err != nil {
		return err
	}

	// Sistema: end-user-app
	endUserApp := enduserapp.NewEndUserAppRouter(&enduserapp.EndUserAppRouterDeps{
		IdentityClient:    r.deps.ID,
		Auth:              r.deps.Auth,
		HeaderIDTokenName: r.deps.HeaderIDTokenName,
	})
	if err := endUserApp.Register(v1); err != nil {
		return err
	}

	// Webhook
	webhook := webhook.NewWebhookRouter(&webhook.WebhookRouterDeps{
		IdentityClient:  r.deps.ID,
		AuthentikApiKey: r.deps.AuthentikApiKey,
	})
	if err := webhook.Register(v1); err != nil {
		return err
	}

	return nil
}
