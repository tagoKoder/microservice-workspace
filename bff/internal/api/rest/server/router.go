package server

import (
	"context"
	"net/http"
	"time"

	"github.com/go-chi/chi/v5"
	chimw "github.com/go-chi/chi/v5/middleware"

	openapi "github.com/tagoKoder/bff/internal/api/rest/gen/openapi"
	"github.com/tagoKoder/bff/internal/api/rest/middleware"
)

type RouterDeps struct {
	Server  *Server
	Swagger any
	HSTS    bool

	AuthPublicPrefixes []string
	CSRFProtected      []string
}

func NewRouter(deps RouterDeps, swaggerValidator func(http.Handler) http.Handler) chi.Router {
	r := chi.NewRouter()

	// --- Baseline
	r.Use(chimw.RealIP)
	r.Use(chimw.RequestID)
	r.Use(middleware.Recover())
	r.Use(middleware.AccessLog())

	// --- Hardening
	r.Use(middleware.SecurityHeaders(deps.HSTS))
	r.Use(middleware.NoStore())
	r.Use(middleware.MaxBodyBytes(12 << 20))
	r.Use(middleware.Timeout(12 * time.Second))

	// --- Schema validation
	r.Use(swaggerValidator)

	// --- Rate limiting
	r.Use(middleware.RateLimit(deps.Server.deps.Config.RateLimitRPS))

	// --- Auth cookie session
	r.Use(middleware.AuthSession(middleware.AuthDeps{
		Config:   deps.Server.deps.Config,
		Identity: deps.Server.deps.Clients.Identity,
		Cookies:  deps.Server.deps.Cookies,
	}, deps.AuthPublicPrefixes))

	// --- CSRF
	r.Use(middleware.CSRF(deps.Server.deps.CSRF, middleware.CSRFRules{
		PublicPrefixes:    deps.AuthPublicPrefixes,
		ProtectedPrefixes: deps.CSRFProtected,
	}))

	// --- OpenAPI STRICT
	strictMw := openapi.StrictMiddlewareFunc(
		func(next openapi.StrictHandlerFunc, operationID string) openapi.StrictHandlerFunc {
			return func(ctx context.Context, w http.ResponseWriter, r *http.Request, request interface{}) (interface{}, error) {
				ctx = middleware.WithHTTP(ctx, w, r)
				return next(ctx, w, r, request)
			}
		},
	)

	strict := openapi.NewStrictHandlerWithOptions(
		deps.Server,
		[]openapi.StrictMiddlewareFunc{strictMw}, // OJO: slice, no func suelto
		openapi.StrictHTTPServerOptions{
			RequestErrorHandlerFunc: func(w http.ResponseWriter, r *http.Request, err error) {
				http.Error(w, err.Error(), http.StatusBadRequest)
			},
			ResponseErrorHandlerFunc: func(w http.ResponseWriter, r *http.Request, err error) {
				http.Error(w, err.Error(), http.StatusInternalServerError)
			},
		},
	)

	openapi.HandlerFromMux(strict, r)

	return r
}
