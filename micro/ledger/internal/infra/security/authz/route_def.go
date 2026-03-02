package authz

type Mode string

const (
	ModePublic    Mode = "PUBLIC"
	ModeAuthnOnly Mode = "AUTHN_ONLY"
	ModeAuthz     Mode = "AUTHZ"
)

type RouteDef struct {
	ActionID            string
	Critical            bool
	Mode                Mode
	RequireCustomerLink bool
	ResourceTemplate    string
}
