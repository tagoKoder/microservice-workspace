package enduserapp

import (
	"github.com/gorilla/mux"

	mw "github.com/tagoKoder/gateway/internal/entrypoint/http/middleware"
	idcli "github.com/tagoKoder/gateway/internal/integration/grpc/identity"

	identrouter "github.com/tagoKoder/gateway/internal/entrypoint/http/v1/end-user-app/feature/identity/router"
)

type EndUserAppRouterDeps struct {
	IdentityClient    idcli.IdentityAPI
	Auth              mw.AccessTokenVerifier
	HeaderIDTokenName string
}

type EndUserAppRouter struct {
	deps *EndUserAppRouterDeps
}

func NewEndUserAppRouter(deps *EndUserAppRouterDeps) *EndUserAppRouter {
	return &EndUserAppRouter{deps: deps}
}

func (r *EndUserAppRouter) Register(parent *mux.Router) error {
	sys := parent.PathPrefix("/end-user-app").Subrouter()
	identrouter.Mount(sys, &identrouter.IdentityRouterDeps{
		IDClient:          r.deps.IdentityClient,
		Auth:              r.deps.Auth,
		HeaderIDTokenName: r.deps.HeaderIDTokenName,
	})
	return nil
}
