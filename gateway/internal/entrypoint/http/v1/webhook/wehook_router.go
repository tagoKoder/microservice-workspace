package webhook

import (
	"github.com/gorilla/mux"

	idcli "github.com/tagoKoder/gateway/internal/integration/grpc/identity"

	authentikrouter "github.com/tagoKoder/gateway/internal/entrypoint/http/v1/webhook/authentik/router"
)

type WebhookRouterDeps struct {
	IdentityClient  idcli.IdentityAPI
	AuthentikApiKey string
}

type WebhookRouter struct {
	deps *WebhookRouterDeps
}

func NewWebhookRouter(deps *WebhookRouterDeps) *WebhookRouter {
	return &WebhookRouter{deps: deps}
}

func (r *WebhookRouter) Register(parent *mux.Router) error {
	sys := parent.PathPrefix("/webhook").Subrouter()
	authentikrouter.Mount(sys, &authentikrouter.AuthentikWebhookRouterDeps{
		IDClient:        r.deps.IdentityClient,
		AuthentikApiKey: r.deps.AuthentikApiKey,
	})
	return nil
}
