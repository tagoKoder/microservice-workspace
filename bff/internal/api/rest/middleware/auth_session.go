package middleware

import (
	"context"
	"net"
	"net/http"
	"strings"

	"github.com/tagoKoder/bff/internal/client/ports"
	"github.com/tagoKoder/bff/internal/config"
	"github.com/tagoKoder/bff/internal/security"
)

type ctxKey string

const (
	CtxSubject     ctxKey = "subject"
	CtxRoles       ctxKey = "roles"
	CtxSession     ctxKey = "session_id"
	CtxCustomer    ctxKey = "customer_id"
	CtxUserState   ctxKey = "user_status"
	CtxIdentity    ctxKey = "identity_id"
	CtxMfaRequired ctxKey = "mfa_required"
	CtxMfaVerified ctxKey = "mfa_verified"
)

type AuthDeps struct {
	Config   config.Config
	Identity ports.IdentityPort
	Cookies  *security.CookieManager
}

func AuthSession(deps AuthDeps, publicPrefixes []string) func(http.Handler) http.Handler {
	isPublic := func(p string) bool {
		for _, pref := range publicPrefixes {
			if strings.HasPrefix(p, pref) {
				return true
			}
		}
		return false
	}

	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {

			if isPublic(r.URL.Path) {
				next.ServeHTTP(w, r)
				return
			}

			sid, ok := deps.Cookies.ReadSessionID(r)
			if !ok {
				w.WriteHeader(http.StatusUnauthorized)
				return
			}

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

			ctx := r.Context()
			ctx = context.WithValue(ctx, CtxSubject, info.SubjectIDOidc)
			ctx = context.WithValue(ctx, CtxRoles, info.User.Roles)
			ctx = context.WithValue(ctx, CtxSession, sid)
			ctx = context.WithValue(ctx, CtxCustomer, info.CustomerID)
			ctx = context.WithValue(ctx, CtxUserState, info.UserStatus)
			ctx = context.WithValue(ctx, CtxIdentity, info.IdentityID)
			ctx = context.WithValue(ctx, CtxMfaRequired, info.MfaRequired)
			ctx = context.WithValue(ctx, CtxMfaVerified, info.MfaVerified)

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
