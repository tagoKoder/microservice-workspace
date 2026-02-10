// bff\internal\api\rest\middleware\access_token.go

package middleware

import (
	"net/http"

	"github.com/tagoKoder/bff/internal/security"
	"github.com/tagoKoder/bff/internal/util"
)

type AccessTokenDeps struct {
	Tokens  *security.AccessTokenProvider
	Cookies *security.CookieManager
}

func AccessToken(deps AccessTokenDeps, oas *OpenAPISecurity) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			ri, ok := oas.Find(r)
			if !ok || !RequiresScheme(ri.Security, "cookieAuth") {
				next.ServeHTTP(w, r)
				return
			}

			// session_id de ctx (set por AuthSession). Fallback a cookie por robustez.
			sid := security.SessionID(r.Context())
			if sid == "" {
				if v, ok := deps.Cookies.ReadSessionID(r); ok {
					sid = v
				}
			}
			if sid == "" {
				w.WriteHeader(http.StatusUnauthorized)
				return
			}

			out, err := deps.Tokens.Ensure(r.Context(), sid, util.GetClientIP(r), r.UserAgent())
			if err != nil {
				// invalida estado local y borra cookie
				deps.Tokens.Invalidate(sid)
				deps.Cookies.Clear(w)
				w.WriteHeader(http.StatusUnauthorized)
				return
			}

			// Si rotó session_id, rota cookie usando SessionExpiresIn (no AccessTokenExpiresIn)
			if out.SessionID != "" && out.SessionID != sid {
				deps.Cookies.Set(w, out.SessionID, out.SessionExpiresIn)
			}

			// coloca en ctx para gRPC interceptor
			ctx := r.Context()
			ctx = security.WithSessionID(ctx, out.SessionID)
			ctx = security.WithAccessToken(ctx, out.AccessToken)

			next.ServeHTTP(w, r.WithContext(ctx))
		})
	}
}
