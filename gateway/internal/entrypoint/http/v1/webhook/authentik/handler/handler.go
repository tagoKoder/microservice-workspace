package handler

import (
	"net/http"

	idcli "github.com/tagoKoder/gateway/internal/integration/grpc/identity"
)

type AuthentikWebhookHandler interface {
	UpsertUser(w http.ResponseWriter, r *http.Request) // POST
}

type AuthentikWebhookHandlerDeps struct {
	IDClient idcli.IdentityAPI
}
type authentikWebhookHandler struct {
	deps *AuthentikWebhookHandlerDeps
}

func NewAuthentikWebhookHandler(d *AuthentikWebhookHandlerDeps) AuthentikWebhookHandler {
	return &authentikWebhookHandler{
		deps: d,
	}
}
