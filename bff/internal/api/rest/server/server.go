// bff\internal\api\rest\server\server.go
package server

import (
	openapi "github.com/tagoKoder/bff/internal/api/rest/gen/openapi"

	accountsH "github.com/tagoKoder/bff/internal/api/rest/v1/web/feature/accounts"
	loginH "github.com/tagoKoder/bff/internal/api/rest/v1/web/feature/login"
	onboardingH "github.com/tagoKoder/bff/internal/api/rest/v1/web/feature/onboarding"
	paymentsH "github.com/tagoKoder/bff/internal/api/rest/v1/web/feature/payments"
	profileH "github.com/tagoKoder/bff/internal/api/rest/v1/web/feature/profile"
	sessionH "github.com/tagoKoder/bff/internal/api/rest/v1/web/feature/session"

	"github.com/tagoKoder/bff/internal/client/ports"
	"github.com/tagoKoder/bff/internal/config"
	"github.com/tagoKoder/bff/internal/security"
)

type Dependencies struct {
	Config   config.Config
	Clients  *ports.Clients
	Cookies  *security.CookieManager
	CSRF     *security.CSRFManager
	Redirect *security.RedirectPolicy
	//TokenCache security.TokenCache
}

type Server struct {
	deps Dependencies

	login      *loginH.Handler
	onboarding *onboardingH.Handler
	session    *sessionH.Handler
	accounts   *accountsH.Handler
	payments   *paymentsH.Handler
	profile    *profileH.Handler
}

func New(deps Dependencies) *Server {
	return &Server{
		deps:       deps,
		login:      loginH.New(deps.Config, deps.Clients, deps.Cookies, deps.CSRF, deps.Redirect),
		onboarding: onboardingH.New(deps.Config, deps.Clients),
		session:    sessionH.New(deps.Config, deps.Clients, deps.Cookies, deps.CSRF),
		accounts:   accountsH.New(deps.Config, deps.Clients),
		payments:   paymentsH.New(deps.Config, deps.Clients),
		profile:    profileH.New(deps.Config, deps.Clients),
	}
}

var _ openapi.StrictServerInterface = (*Server)(nil)
