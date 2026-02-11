// bff\internal\api\rest\middleware\route_template.go
package middleware

import (
	"context"
	"net/http"
)

const CtxRouteTemplate ctxKey = "route_template"

func RouteTemplate(oas *OpenAPISecurity) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			if ri, ok := oas.Find(r); ok {
				ctx := context.WithValue(r.Context(), CtxRouteTemplate, ri.RouteTemplate)
				next.ServeHTTP(w, r.WithContext(ctx))
				return
			}
			next.ServeHTTP(w, r)
		})
	}
}

func GetRouteTemplate(ctx context.Context) string {
	v, _ := ctx.Value(CtxRouteTemplate).(string)
	return v
}
