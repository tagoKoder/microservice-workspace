// bff\cmd\bff\main.go
package main

import (
	"log"
	"net/http"
	"time"

	openapi "github.com/tagoKoder/bff/internal/api/rest/gen/openapi"
	restserver "github.com/tagoKoder/bff/internal/api/rest/server"

	"github.com/tagoKoder/bff/internal/client/grpc"
	"github.com/tagoKoder/bff/internal/config"
	"github.com/tagoKoder/bff/internal/security"

	oapimw "github.com/deepmap/oapi-codegen/pkg/chi-middleware"
	"github.com/getkin/kin-openapi/openapi3filter"
)

func main() {
	cfg := config.Load()

	// ---- gRPC clients
	clients, err := grpc.NewClients(cfg)
	if err != nil {
		log.Fatalf("grpc clients: %v", err)
	}
	defer func() { _ = clients.Close() }()

	// ---- OpenAPI swagger
	swagger, err := openapi.GetSwagger()
	if err != nil {
		log.Fatalf("swagger: %v", err)
	}
	swagger.Servers = nil // evita validación por servers

	// ---- Validator (auth noop; la auth real está en middleware.AuthSession)
	validator := oapimw.OapiRequestValidatorWithOptions(swagger, &oapimw.Options{
		Options: openapi3filter.Options{
			AuthenticationFunc: openapi3filter.NoopAuthenticationFunc,
		},
	})

	cookies := security.NewCookieManager(cfg)
	csrf := security.NewCSRFManager(cfg)
	redirect := security.NewRedirectPolicy(cfg.RedirectAllowlist)
	tokens := security.NewAccessTokenProvider(clients.Identity)

	srv := restserver.New(restserver.Dependencies{
		Config:   cfg,
		Clients:  clients,
		Cookies:  cookies,
		CSRF:     csrf,
		Redirect: redirect,
		Tokens:   tokens,
	})

	router := restserver.NewRouter(
		restserver.RouterDeps{
			Server: srv,
			HSTS:   cfg.CookieSecure,
		},
		swagger,
		validator,
	)

	httpSrv := &http.Server{
		Addr:              cfg.HTTPAddr,
		Handler:           router,
		ReadTimeout:       10 * time.Second,
		ReadHeaderTimeout: 10 * time.Second,
		WriteTimeout:      20 * time.Second,
		IdleTimeout:       60 * time.Second,
	}

	log.Printf("BFF listening on %s (env=%s)", cfg.HTTPAddr, cfg.Env)
	log.Fatal(httpSrv.ListenAndServe())
}
