package middleware

import (
	"context"
	"net/http"
)

const (
	CtxHTTPRequest        ctxKey = "http_request"
	CtxHTTPResponseWriter ctxKey = "http_response_writer"
)

func WithHTTP(ctx context.Context, w http.ResponseWriter, r *http.Request) context.Context {
	ctx = context.WithValue(ctx, CtxHTTPRequest, r)
	ctx = context.WithValue(ctx, CtxHTTPResponseWriter, w)
	return ctx
}

func GetHTTPRequest(ctx context.Context) (*http.Request, bool) {
	r, ok := ctx.Value(CtxHTTPRequest).(*http.Request)
	return r, ok
}

func GetHTTPResponseWriter(ctx context.Context) (http.ResponseWriter, bool) {
	w, ok := ctx.Value(CtxHTTPResponseWriter).(http.ResponseWriter)
	return w, ok
}
