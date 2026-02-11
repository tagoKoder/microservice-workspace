// bff\internal\api\rest\server\router.go
package server

import (
	"net/http"

	"github.com/getkin/kin-openapi/openapi3"
	"github.com/go-chi/chi/v5"
	chimw "github.com/go-chi/chi/v5/middleware"

	openapi "github.com/tagoKoder/bff/internal/api/rest/gen/openapi"
	"github.com/tagoKoder/bff/internal/api/rest/middleware"
)

type RouterDeps struct {
	Server *Server
	HSTS   bool
}

func NewRouter(deps RouterDeps, swagger *openapi3.T, swaggerValidator func(http.Handler) http.Handler) chi.Router {
	r := chi.NewRouter()

	// Resolver OpenAPI para seguridad por operación
	oas, err := middleware.NewOpenAPISecurity(swagger)
	if err != nil {
		panic(err)
	}

	// 1) Context HTTP disponible desde el inicio
	r.Use(middleware.WithHTTPMiddleware)

	// 2) Correlation-ID estándar
	r.Use(middleware.CorrelationID)

	// 3) baseline/hardening
	r.Use(chimw.RealIP)
	r.Use(middleware.Recover())
	r.Use(middleware.RouteTemplate(oas))
	r.Use(middleware.AccessLog())
	r.Use(middleware.SecurityHeaders(deps.HSTS))

	// 4) Schema validation (si lo pasas)
	if swaggerValidator != nil {
		r.Use(swaggerValidator)
	}

	// 5) Rate limiting
	r.Use(middleware.RateLimit(deps.Server.deps.Config.RateLimitRPS))

	// 6) Auth cookie session (OpenAPI-driven)
	r.Use(middleware.AuthSession(middleware.AuthDeps{
		Identity: deps.Server.deps.Clients.Identity,
		Cookies:  deps.Server.deps.Cookies,
		Tokens:   deps.Server.deps.Tokens,
	}, oas))

	r.Use(middleware.AccessToken(
		middleware.AccessTokenDeps{
			Tokens:  deps.Server.deps.Tokens,
			Cookies: deps.Server.deps.Cookies,
		},
		oas))

	// 7) CSRF (OpenAPI-driven)
	r.Use(middleware.CSRF(deps.Server.deps.CSRF, oas))

	// 8) OpenAPI STRICT handler
	strict := openapi.NewStrictHandlerWithOptions(
		deps.Server,
		nil, // no strict middleware needed
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
