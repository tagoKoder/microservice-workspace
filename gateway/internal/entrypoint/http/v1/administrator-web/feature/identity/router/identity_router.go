package router

import (
	"net/http"

	"github.com/gorilla/mux"

	mw "github.com/tagoKoder/gateway/internal/entrypoint/http/middleware"
	idcli "github.com/tagoKoder/gateway/internal/integration/grpc/identity"

	"github.com/tagoKoder/gateway/internal/entrypoint/http/v1/administrator-web/feature/identity/handler"
	"github.com/tagoKoder/gateway/internal/entrypoint/http/v1/administrator-web/feature/identity/router/constant"
)

type AdminIdentityRouterDeps struct {
	IDClient          idcli.IdentityAPI
	Auth              mw.AccessTokenVerifier
	HeaderIDTokenName string
}

// Mount registra las rutas del feature bajo /identity.
// Aplica auth SÓLO a las que requieren access token.
func Mount(parent *mux.Router, deps *AdminIdentityRouterDeps) {
	base := parent.PathPrefix("/identity").Subrouter()
	h := handler.NewIdentityHandler(&handler.AdminIdentityDeps{IDClient: deps.IDClient, HeaderIDTokenName: deps.HeaderIDTokenName})

	// Rutas públicas
	base.HandleFunc(constant.IdentityLinkRoute, h.Link).Methods(http.MethodPost, http.MethodOptions)

	// Rutas protegidas
	protected := base.NewRoute().Subrouter()
	protected.Use(deps.Auth.Verify)
	protected.HandleFunc(constant.IdentityWhoAmIRoute, h.WhoAmI).Methods(http.MethodGet, http.MethodOptions)
}
