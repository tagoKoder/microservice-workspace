package identity

import (
	"encoding/json"
	"net/http"

	"github.com/tagoKoder/gateway/internal/entrypoint/http/middleware"
	idcli "github.com/tagoKoder/gateway/internal/integration/grpc/identity"
)

type Handler struct {
	ID   *idcli.Client
	OIDC *middleware.OIDC
}

func New(ID *idcli.Client, oidc *middleware.OIDC) *Handler {
	return &Handler{ID: ID, OIDC: oidc}
}

func (h *Handler) Register(r *http.ServeMux) {
	// GET /api/v1/administrator-web/identity/whoami  (ACCESS TOKEN)
	r.Handle("/api/v1/administrator-web/identity/whoami",
		h.OIDC.Auth(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			access := r.Context().Value(middleware.AccessTokenKey).(string)
			res, err := h.ID.WhoAmI(r.Context(), access)
			if err != nil {
				http.Error(w, err.Error(), http.StatusBadGateway)
				return
			}
			_ = json.NewEncoder(w).Encode(res)
		})),
	)

	// POST /api/v1/administrator-web/identity/link  (ID TOKEN via header)
	r.Handle("/api/v1/administrator-web/identity/link",
		http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			idToken := r.Header.Get("X-ID-Token")
			if idToken == "" {
				http.Error(w, "missing X-ID-Token", http.StatusBadRequest)
				return
			}
			res, err := h.ID.Link(r.Context(), idToken)
			if err != nil {
				http.Error(w, err.Error(), http.StatusBadGateway)
				return
			}
			_ = json.NewEncoder(w).Encode(res)
		}),
	)
}
