// bff\internal\api\rest\middleware\auth_session.go

package middleware

import (
	"context"
	"log"
	"net/http"

	"github.com/tagoKoder/bff/internal/client/ports"
	"github.com/tagoKoder/bff/internal/config"
	"github.com/tagoKoder/bff/internal/security"
	"github.com/tagoKoder/bff/internal/util"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
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
				// 🔎 Debug útil: mira si realmente vino Cookie header
				// (no loguees el valor completo del SID; es secreto)
				log.Printf("missing sid cookie: cookieHeader=%q expectedName=%s host=%s", r.Header.Get("Cookie"), deps.Cookies.Name(), r.Host)
				w.WriteHeader(http.StatusUnauthorized)
				return
			}

			ctx := r.Context()
			ctx = security.WithSessionID(ctx, sid)

			info, err := deps.Identity.GetSessionInfo(ctx, ports.GetSessionInfoInput{
				SessionID: sid,
				IP:        util.GetClientIP(r),
				UserAgent: r.UserAgent(),
			})
			if err != nil {
				// ✅ Clasifica el error: ¿sesión inválida o Identity caído?
				if st, ok := status.FromError(err); ok {
					switch st.Code() {
					case codes.Unauthenticated, codes.NotFound, codes.FailedPrecondition:
						// Sesión inválida/expirada/revocada → sí borrar cookie
						deps.Cookies.Clear(w)
						w.WriteHeader(http.StatusUnauthorized)
						return
					case codes.Unavailable, codes.DeadlineExceeded:
						// Infra temporal → NO borrar cookie
						http.Error(w, "identity unavailable", http.StatusServiceUnavailable)
						return
					default:
						// Otros errores → no te auto-dispares borrando sesión
						http.Error(w, "identity error", http.StatusBadGateway)
						return
					}
				}

				// Error no-gRPC: tratar como infraestructura
				http.Error(w, "identity error", http.StatusBadGateway)
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
			ctx = context.WithValue(ctx, ctxKey("route_template"), ri.RouteTemplate)

			next.ServeHTTP(w, r.WithContext(ctx))
		})
	}
}
