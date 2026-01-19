// bff/internal/api/rest/middleware/openapi_security.go
package middleware

import (
	"net/http"
	"strings"

	"github.com/getkin/kin-openapi/openapi3"
	"github.com/getkin/kin-openapi/routers"
	"github.com/getkin/kin-openapi/routers/legacy"
)

type OpenAPISecurity struct {
	swagger *openapi3.T
	router  routers.Router
}

type RouteInfo struct {
	OperationID   string
	RouteTemplate string
	Security      openapi3.SecurityRequirements
}

func NewOpenAPISecurity(swagger *openapi3.T) (*OpenAPISecurity, error) {
	r, err := legacy.NewRouter(swagger)
	if err != nil {
		return nil, err
	}
	return &OpenAPISecurity{swagger: swagger, router: r}, nil
}

// EffectiveSecurity implementa semántica OpenAPI:
// - nil => usa security global
// - empty slice => sin auth (público)
func (s *OpenAPISecurity) effectiveSecurity(op *openapi3.Operation) openapi3.SecurityRequirements {
	if op == nil {
		return nil
	}
	if op.Security == nil {
		return s.swagger.Security
	}
	return *op.Security
}

func (s *OpenAPISecurity) Find(r *http.Request) (*RouteInfo, bool) {
	route, _, err := s.router.FindRoute(r)
	if err != nil || route == nil || route.Operation == nil {
		return nil, false
	}

	sec := s.effectiveSecurity(route.Operation)

	return &RouteInfo{
		OperationID:   route.Operation.OperationID,
		RouteTemplate: route.Path, // template OpenAPI: /api/v1/accounts/{id}
		Security:      sec,
	}, true
}

// Un “security requirement object” es AND.
// Múltiples objects es OR.
// Para “requiere esquema X”, basta que exista algún object que lo contenga.
func RequiresScheme(sec openapi3.SecurityRequirements, scheme string) bool {
	if sec == nil {
		return false
	}
	for _, req := range sec {
		for k := range req {
			if strings.EqualFold(k, scheme) {
				return true
			}
		}
	}
	return false
}

// Para CSRF queremos AND: cookieAuth + csrfAuth en el mismo requirement object.
func RequiresBothInSameRequirement(sec openapi3.SecurityRequirements, a, b string) bool {
	if sec == nil {
		return false
	}
	for _, req := range sec {
		_, okA := req[a]
		_, okB := req[b]
		if okA && okB {
			return true
		}
	}
	return false
}
