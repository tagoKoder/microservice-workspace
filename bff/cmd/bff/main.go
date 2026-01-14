package main

import (
	"log"
	"net/http"
	"time"

	openapi "github.com/tagoKoder/bff/internal/api/rest/gen/openapi"
	"github.com/tagoKoder/bff/internal/api/rest/middleware"
	restserver "github.com/tagoKoder/bff/internal/api/rest/server"

	"github.com/tagoKoder/bff/internal/client/grpc"
	"github.com/tagoKoder/bff/internal/config"
	"github.com/tagoKoder/bff/internal/security"
)

func main() {
	cfg := config.Load()

	// ---- gRPC clients (tu NewClients(cfg) ya existe)
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
	swagger.Servers = nil // evita validación por "servers"

	cookies := security.NewCookieManager(cfg)
	csrf := security.NewCSRFManager(cfg)
	redirect := security.NewRedirectPolicy(cfg.RedirectAllowlist)

	srv := restserver.New(restserver.Dependencies{
		Config:   cfg,
		Clients:  clients,
		Cookies:  cookies,
		CSRF:     csrf,
		Redirect: redirect,
	})

	// Prefijos públicos según tu OpenAPI (security: [])
	authPublic := []string{
		"/api/v1/onboarding", // intents/verify/consents/activate (si los dejas públicos)
		"/api/v1/login",
	}

	csrfProtected := []string{
		"/api/v1/accounts",
		"/api/v1/payments",
		"/api/v1/beneficiaries",
		"/api/v1/profile",
		"/api/v1/admin/sandbox",
		"/bff/session", // logout
	}

	router := restserver.NewRouter(
		restserver.RouterDeps{
			Server:             srv,
			HSTS:               cfg.CookieSecure, // si CookieSecure implica HTTPS, activa HSTS
			AuthPublicPrefixes: authPublic,
			CSRFProtected:      csrfProtected,
		},
		middleware.OapiValidator(swagger),
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
