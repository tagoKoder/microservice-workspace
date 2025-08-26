package handler

import (
	"net/http"

	"github.com/tagoKoder/gateway/pkg/httpx"
	pkgHttp "github.com/tagoKoder/gateway/pkg/httpx"
	identitypb "github.com/tagoKoder/proto/genproto/go/identity"
)

func (h *authentikWebhookHandler) UpsertUser(w http.ResponseWriter, r *http.Request) {
	req, errPost := httpx.DecodeJSONFromBody[identitypb.AuthentikUserUpsertRequest](r)
	if errPost != nil {
		pkgHttp.Error(w, http.StatusBadRequest, errPost.Error(), "")
		return
	}

	err := h.deps.IDClient.UpsertFromAuthentik(r.Context(), req)
	if err != nil {
		pkgHttp.Error(w, http.StatusBadGateway, err.Error(), "")
		return
	}
	pkgHttp.JSON(w, http.StatusOK, nil)
}
