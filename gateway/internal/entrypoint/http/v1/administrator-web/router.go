package adminweb

import (
	"github.com/gorilla/mux"

	mw "github.com/tagoKoder/gateway/internal/entrypoint/http/middleware"
	idcli "github.com/tagoKoder/gateway/internal/integration/grpc/identity"

	identrouter "github.com/tagoKoder/gateway/internal/entrypoint/http/v1/administrator-web/feature/identity/router"
)

type AdminWebRouterDeps struct {
	IdentityClient    idcli.IdentityAPI
	Auth              mw.AccessTokenVerifier
	HeaderIDTokenName string
}

type AdminWebRouter struct {
	deps *AdminWebRouterDeps
}

func NewAdminWebRouter(deps *AdminWebRouterDeps) *AdminWebRouter {
	return &AdminWebRouter{deps: deps}
}

func (r *AdminWebRouter) Register(parent *mux.Router) error {
	sys := parent.PathPrefix("/administrator-web").Subrouter()
	identrouter.Mount(sys, &identrouter.AdminIdentityRouterDeps{
		IDClient:          r.deps.IdentityClient,
		Auth:              r.deps.Auth,
		HeaderIDTokenName: r.deps.HeaderIDTokenName,
	})
	return nil
}
