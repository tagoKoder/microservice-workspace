// bff\internal\api\rest\middleware\httpctx.go

package middleware

import (
	"context"
	"net/http"
)

type ctxKeyHTTP int

const (
	ctxKeyHTTPRequest ctxKeyHTTP = iota + 1
	ctxKeyHTTPResponseWriter
)

// WithHTTP inyecta http.ResponseWriter y *http.Request en el contexto (para strict handlers).
func WithHTTP(ctx context.Context, w http.ResponseWriter, r *http.Request) context.Context {
	ctx = context.WithValue(ctx, ctxKeyHTTPRequest, r)
	ctx = context.WithValue(ctx, ctxKeyHTTPResponseWriter, w)
	return ctx
}

// GetHTTPRequest obtiene el *http.Request desde el contexto.
func GetHTTPRequest(ctx context.Context) (*http.Request, bool) {
	v := ctx.Value(ctxKeyHTTPRequest)
	if v == nil {
		return nil, false
	}
	r, ok := v.(*http.Request)
	return r, ok && r != nil
}

// GetHTTPResponseWriter obtiene el http.ResponseWriter desde el contexto.
func GetHTTPResponseWriter(ctx context.Context) (http.ResponseWriter, bool) {
	v := ctx.Value(ctxKeyHTTPResponseWriter)
	if v == nil {
		return nil, false
	}
	w, ok := v.(http.ResponseWriter)
	return w, ok && w != nil
}

func WithHTTPMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		ctx := WithHTTP(r.Context(), w, r)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}
