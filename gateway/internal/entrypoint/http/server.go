package http

import (
	"context"
	"net/http"
	"os"
	"time"

	adminID "github.com/tagoKoder/gateway/internal/entrypoint/http/v1/administrator-web/identity"

	"github.com/tagoKoder/gateway/internal/adapter/logger"
	"github.com/tagoKoder/gateway/internal/entrypoint/http/middleware"
	idcli "github.com/tagoKoder/gateway/internal/integration/grpc/identity"

	"github.com/gorilla/mux"
	"github.com/rs/zerolog/hlog"
)

type Server struct {
	httpSrv *http.Server
	id      *idcli.Client
}

func NewServer(ctx context.Context) (*Server, error) {
	log := logger.New()

	issuer := os.Getenv("OIDC_ISSUER")        // ej: http://localhost:9000/application/o/administrator-web/
	idAddr := os.Getenv("IDENTITY_GRPC_ADDR") // ej: localhost:8081
	httpAddr := os.Getenv("HTTP_ADDR")        // ej: :8080
	if httpAddr == "" {
		httpAddr = ":8080"
	}

	oidc, err := middleware.NewOIDC(ctx, issuer)
	if err != nil {
		return nil, err
	}

	id, err := idcli.New(idAddr)
	if err != nil {
		return nil, err
	}

	// Router principal
	r := mux.NewRouter()

	// Middlewares globales
	r.Use(mux.CORSMethodMiddleware(r))
	r.Use(func(next http.Handler) http.Handler { return hlog.NewHandler(log)(next) })
	r.Use(middleware.RequestID)
	r.Use(middleware.Recover)
	r.Use(middleware.CORS)

	// Health
	r.HandleFunc("/healthz", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte(`{"ok":true}`))
	}).Methods(http.MethodGet, http.MethodOptions)

	// Sub-mux para montar handlers de cada sistema/versión
	inner := http.NewServeMux()

	// v1/administrator-web
	adminID.New(id, oidc).Register(inner)

	// Redirige todo /api/* hacia el serve mux interno
	r.PathPrefix("/api/").Handler(inner)

	srv := &http.Server{
		Addr:              httpAddr,
		Handler:           r,
		ReadHeaderTimeout: 5 * time.Second,
	}

	return &Server{httpSrv: srv, id: id}, nil
}

func (s *Server) Start() error {
	return s.httpSrv.ListenAndServe()
}

func (s *Server) Shutdown(ctx context.Context) error {
	if s.id != nil {
		s.id.Close()
	}
	return s.httpSrv.Shutdown(ctx)
}
