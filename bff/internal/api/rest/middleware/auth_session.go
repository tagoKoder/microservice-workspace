package middleware

import (
	"context"
	"net"
	"net/http"

	"github.com/tagoKoder/bff/internal/client/ports"
	"github.com/tagoKoder/bff/internal/config"
	"github.com/tagoKoder/bff/internal/security"
)

const (
	CtxSubject   ctxKey = "subject"
	CtxRoles     ctxKey = "roles"
	CtxSession   ctxKey = "session_id"
	CtxCustomer  ctxKey = "customer_id"
	CtxUserState ctxKey = "user_status"
	CtxIdentity  ctxKey = "identity_id"
)

type AuthDeps struct {
	Config   config.Config
	Identity ports.IdentityPort
	Cookies  *security.CookieManager
}

func AuthSession(deps AuthDeps, oas *OpenAPISecurity) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			ri, ok := oas.Find(r)
			if !ok || !RequiresScheme(ri.Security, "cookieAuth") {
				next.ServeHTTP(w, r)
				return
			}

			sid, ok := deps.Cookies.ReadSessionID(r)
			if !ok || sid == "" {
				w.WriteHeader(http.StatusUnauthorized)
				return
			}

			ctx := r.Context()
			ctx = security.WithSessionID(ctx, sid)

			// Nota: si luego quieres hash de IP/UA, hazlo aquí, pero cuida compatibilidad con Identity.
			info, err := deps.Identity.GetSessionInfo(r.Context(), ports.GetSessionInfoInput{
				SessionID: sid,
				IP:        clientIP(r),
				UserAgent: r.UserAgent(),
			})
			if err != nil {
				deps.Cookies.Clear(w)
				w.WriteHeader(http.StatusUnauthorized)
				return
			}
			if info.UserStatus != "" && info.UserStatus != "ACTIVE" {
				w.WriteHeader(http.StatusForbidden)
				return
			}

			ctx = context.WithValue(ctx, CtxSubject, info.SubjectIDOidc)
			ctx = context.WithValue(ctx, CtxRoles, info.User.Roles)
			ctx = context.WithValue(ctx, CtxSession, sid)
			ctx = context.WithValue(ctx, CtxCustomer, info.CustomerID)
			ctx = context.WithValue(ctx, CtxUserState, info.UserStatus)
			ctx = context.WithValue(ctx, CtxIdentity, info.IdentityID)

			// Route template en contexto (te sirve para auditoría y ActionResolver)
			ctx = context.WithValue(ctx, ctxKey("route_template"), ri.RouteTemplate)

			next.ServeHTTP(w, r.WithContext(ctx))
		})
	}
}

func clientIP(r *http.Request) string {
	// Si estás detrás de proxy, Chi RealIP te setea RemoteAddr con el real
	host, _, err := net.SplitHostPort(r.RemoteAddr)
	if err != nil {
		return r.RemoteAddr
	}
	return host
}
