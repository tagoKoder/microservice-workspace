package ports

import "context"

type OidcUser struct {
	Name  string
	Email string
	Roles []string
}

type StartOidcLoginInput struct {
	Channel            string
	RedirectAfterLogin string
}
type StartOidcLoginOutput struct {
	AuthorizationURL string
	State            string
}

type CompleteOidcLoginInput struct {
	Code      string
	State     string
	IP        string
	UserAgent string
	Channel   string
}
type CompleteOidcLoginOutput struct {
	IdentityID         string
	SubjectIDOidc      string
	Provider           string
	User               OidcUser
	SessionID          string
	SessionExpiresIn   int64
	RedirectAfterLogin string

	MfaRequired bool
	MfaVerified bool
}

type RefreshSessionInput struct {
	SessionID string
	IP        string
	UserAgent string
}
type RefreshSessionOutput struct {
	SessionID        string
	SessionExpiresIn int64
}

type LogoutSessionInput struct {
	SessionID string
}
type LogoutSessionOutput struct {
	Success bool
}

type GetSessionInfoInput struct {
	SessionID string
	IP        string
	UserAgent string
}
type GetSessionInfoOutput struct {
	IdentityID       string
	SubjectIDOidc    string
	Provider         string
	User             OidcUser
	CustomerID       string
	UserStatus       string // ACTIVE|LOCKED|DISABLED
	SessionExpiresIn int64
	MfaRequired      bool
	MfaVerified      bool
}

type StartRegistrationInput struct {
	Channel             string
	NationalID          string
	NationalIDIssueDate string
	FingerprintCode     string

	IdDocumentFront []byte
	Selfie          []byte

	MonthlyIncome  float64
	OccupationType string // student|employee|self_employed|unemployed|retired
	Email          string
	Phone          string
}

type StartRegistrationOutput struct {
	RegistrationID   string
	State            string
	CreatedAtRFC3339 string
}

// -------------------
// WebAuthn (proto WebauthnService)
// -------------------

type BeginWebauthnRegistrationInput struct {
	IdentityID string
	DeviceName string // opcional (empty == none)
}
type BeginWebauthnRegistrationOutput struct {
	RequestID   string
	OptionsJSON string
}

type FinishWebauthnRegistrationInput struct {
	RequestID      string
	CredentialJSON string
}
type FinishWebauthnRegistrationOutput struct {
	Success bool
}

type BeginWebauthnAssertionInput struct {
	IdentityID string
	SessionID  string
}
type BeginWebauthnAssertionOutput struct {
	RequestID   string
	OptionsJSON string
}

type FinishWebauthnAssertionInput struct {
	SessionID      string
	RequestID      string
	CredentialJSON string
}
type FinishWebauthnAssertionOutput struct {
	Success bool
}

type IdentityPort interface {
	StartOidcLogin(ctx context.Context, in StartOidcLoginInput) (StartOidcLoginOutput, error)
	CompleteOidcLogin(ctx context.Context, in CompleteOidcLoginInput) (CompleteOidcLoginOutput, error)
	RefreshSession(ctx context.Context, in RefreshSessionInput) (RefreshSessionOutput, error)
	LogoutSession(ctx context.Context, in LogoutSessionInput) (LogoutSessionOutput, error)
	GetSessionInfo(ctx context.Context, in GetSessionInfoInput) (GetSessionInfoOutput, error)

	StartRegistration(ctx context.Context, in StartRegistrationInput) (StartRegistrationOutput, error)

	BeginWebauthnRegistration(ctx context.Context, in BeginWebauthnRegistrationInput) (BeginWebauthnRegistrationOutput, error)
	FinishWebauthnRegistration(ctx context.Context, in FinishWebauthnRegistrationInput) (FinishWebauthnRegistrationOutput, error)
	BeginWebauthnAssertion(ctx context.Context, in BeginWebauthnAssertionInput) (BeginWebauthnAssertionOutput, error)
	FinishWebauthnAssertion(ctx context.Context, in FinishWebauthnAssertionInput) (FinishWebauthnAssertionOutput, error)
}
