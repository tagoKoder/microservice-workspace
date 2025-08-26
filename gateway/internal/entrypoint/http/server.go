package http

import (
	"context"
	"net/http"
	"time"

	"github.com/gorilla/mux"
	commonLog "github.com/tagoKoder/common-kit/pkg/logging"
	commonHttpx "github.com/tagoKoder/common-kit/pkg/observability/httpx"
	"github.com/tagoKoder/gateway/internal/config"
	mw "github.com/tagoKoder/gateway/internal/entrypoint/http/middleware"
	v1 "github.com/tagoKoder/gateway/internal/entrypoint/http/v1"
	oidcpkg "github.com/tagoKoder/gateway/pkg/oidc"
)

type HttpServer struct {
	httpSrv   *http.Server
	shutdowns []func()
}

func NewHttpServer(ctx context.Context, cfg *config.Config, clientProvider *ClientProvider) (*HttpServer, error) {

	// ---- Auth global (construido una sola vez) ----
	ver, err := oidcpkg.NewVerifier(ctx, cfg.OidcIssuer, oidcpkg.OidcOptions{SkipClientIDCheck: true})
	if err != nil {
		return nil, err
	}
	auth := mw.NewOIDCAccessTokenVerifier(ver.VerifyAccessToken)

	// ---- Router raíz ----
	r := mux.NewRouter()

	// Middlewares globales
	r.Use(mux.CORSMethodMiddleware(r))
	r.Use(commonLog.RequestLogger)
	r.Use(mw.Recover)
	r.Use(mw.CORS(mw.CORSOptions{
		AllowedOrigins:   cfg.WhiteListOrigin,
		AllowedHeaders:   []string{"Authorization", "Content-Type", cfg.HeaderIDTokenName},
		AllowedMethods:   []string{"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"},
		AllowCredentials: false,
		MaxAgeSeconds:    600,
	}))

	// Health
	r.HandleFunc("/healthz", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte(`{"ok":true}`))
	}).Methods(http.MethodGet, http.MethodOptions)

	// ---- /api ----
	api := r.PathPrefix("/api").Subrouter()

	// ---- v1 ----
	v1Router := v1.NewRouter(v1.V1RouterDeps{
		Auth:            auth,
		ID:              clientProvider.IdentityProvider().Identity(),
		AuthentikApiKey: cfg.AuthentikApiKey,
	})
	if err := v1Router.Register(api); err != nil {
		return nil, err
	}

	// Si mañana hay v2:
	// v2Router := v2.NewRouter(v2.Deps{ Services: v2Prov.Services(), Auth: auth })
	// _ = v2Router.Register(api)

	handler := commonHttpx.Wrap(r, commonHttpx.HttpxWrapperOptions{
		Operation:       "gateway-http",
		WaitForDelivery: true,
	})

	srv := &http.Server{
		Addr:              ":" + cfg.HttpPort,
		Handler:           handler,
		ReadHeaderTimeout: 5 * time.Second,
	}

	shutdowns := []func(){
		func() { _ = clientProvider.Close() },
	}

	return &HttpServer{httpSrv: srv, shutdowns: shutdowns}, nil
}

func (s *HttpServer) Start() error {
	return s.httpSrv.ListenAndServe()
}

func (s *HttpServer) Shutdown(ctx context.Context) error {
	for _, fn := range s.shutdowns {
		fn()
	}
	return s.httpSrv.Shutdown(ctx)
}
