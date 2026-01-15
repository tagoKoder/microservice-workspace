package handler

import (
	"net/http"

	pkgHttp "github.com/tagoKoder/gateway/pkg/httpx"
	pkgOMiddleware "github.com/tagoKoder/gateway/pkg/middleware"
)

func (h *identityHandler) WhoAmI(w http.ResponseWriter, r *http.Request) {
	access, ok := pkgOMiddleware.AccessTokenFrom(r.Context())
	if !ok || access == "" {
		pkgHttp.Error(w, http.StatusUnauthorized, "missing access token", "")
		return
	}
	res, err := h.deps.IDClient.WhoAmI(r.Context(), access)
	if err != nil {
		pkgHttp.Error(w, http.StatusBadGateway, err.Error(), "")
		return
	}
	pkgHttp.JSON(w, http.StatusOK, res)
}
