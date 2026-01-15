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
type IdentityDeps struct {
	IDClient          idcli.IdentityAPI
	HeaderIDTokenName string
}

// implementación concreta
type identityHandler struct {
	deps *IdentityDeps
}

func NewIdentityHandler(d *IdentityDeps) IdentityHandler {
	return &identityHandler{
		deps: d,
	}
}
