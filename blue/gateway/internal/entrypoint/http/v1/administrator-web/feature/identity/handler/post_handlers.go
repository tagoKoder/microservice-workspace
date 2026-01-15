package handler

import (
	"net/http"

	pkgHttp "github.com/tagoKoder/gateway/pkg/httpx"
)

func (h *identityHandler) Link(w http.ResponseWriter, r *http.Request) {
	idToken := r.Header.Get(h.deps.HeaderIDTokenName)
	if idToken == "" {
		pkgHttp.Error(w, http.StatusBadRequest, "missing "+h.deps.HeaderIDTokenName, "")
		return
	}
	res, err := h.deps.IDClient.Link(r.Context(), idToken)
	if err != nil {
		pkgHttp.Error(w, http.StatusBadGateway, err.Error(), "")
		return
	}
	pkgHttp.JSON(w, http.StatusOK, res)
}
