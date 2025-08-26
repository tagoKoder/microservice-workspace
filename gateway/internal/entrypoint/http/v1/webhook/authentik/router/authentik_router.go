package router

import (
	"net/http"

	"github.com/gorilla/mux"
	mw "github.com/tagoKoder/gateway/internal/entrypoint/http/middleware"

	idcli "github.com/tagoKoder/gateway/internal/integration/grpc/identity"

	"github.com/tagoKoder/gateway/internal/entrypoint/http/v1/webhook/authentik/handler"
	"github.com/tagoKoder/gateway/internal/entrypoint/http/v1/webhook/authentik/router/constant"
)

type AuthentikWebhookRouterDeps struct {
	IDClient        idcli.IdentityAPI
	AuthentikApiKey string
}

// Mount registra las rutas del feature bajo /identity.
// Aplica auth SÓLO a las que requieren access token.
func Mount(parent *mux.Router, deps *AuthentikWebhookRouterDeps) {
	base := parent.PathPrefix("/authentik").Subrouter()
	base.Use(mw.AuthentikWebhookVerifier(&mw.AuthentikWebhookOptions{
		Secrets:    []string{deps.AuthentikApiKey},
		HeaderName: "Authorization",
		Prefix:     "Bearer ",
	}))
	h := handler.NewAuthentikWebhookHandler(&handler.AuthentikWebhookHandlerDeps{IDClient: deps.IDClient})

	base.HandleFunc(constant.UserUpsert, h.UpsertUser).Methods(http.MethodPost, http.MethodOptions)
}
