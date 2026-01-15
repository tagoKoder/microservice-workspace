package handler

import (
	"net/http"

	idcli "github.com/tagoKoder/gateway/internal/integration/grpc/identity"
)

// IdentityHandler expone las rutas del feature.
type IdentityHandler interface {
	WhoAmI(w http.ResponseWriter, r *http.Request) // GET
	Link(w http.ResponseWriter, r *http.Request)   // POST
}

// Dependencies Wrapper
type AdminIdentityDeps struct {
	IDClient          idcli.IdentityAPI
	HeaderIDTokenName string
}

// implementación concreta
type identityHandler struct {
	deps *AdminIdentityDeps
}

func NewIdentityHandler(d *AdminIdentityDeps) IdentityHandler {
	return &identityHandler{
		deps: d,
	}
}
