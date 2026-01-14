package grpc

import (
	"context"
	"strings"
	"time"

	identityv1 "github.com/tagoKoder/bff/internal/client/gen/protobuf/bank/identity/v1"
	"github.com/tagoKoder/bff/internal/client/ports"
	"google.golang.org/grpc"
	"google.golang.org/protobuf/types/known/timestamppb"
)

var _ ports.IdentityPort = (*IdentityClient)(nil)

type IdentityClient struct {
	oidc       identityv1.OidcAuthServiceClient
	onboarding identityv1.OnboardingServiceClient
	webauthn   identityv1.WebauthnServiceClient
	timeout    time.Duration
}

func NewIdentityClient(conn *grpc.ClientConn) *IdentityClient {
	return &IdentityClient{
		oidc:       identityv1.NewOidcAuthServiceClient(conn),
		onboarding: identityv1.NewOnboardingServiceClient(conn),
		webauthn:   identityv1.NewWebauthnServiceClient(conn),
		timeout:    5 * time.Second,
	}
}

func (c *IdentityClient) StartOidcLogin(ctx context.Context, in ports.StartOidcLoginInput) (ports.StartOidcLoginOutput, error) {
	ctx2, cancel := context.WithTimeout(ctx, c.timeout)
	defer cancel()

	res, err := c.oidc.StartOidcLogin(ctx2, &identityv1.StartOidcLoginRequest{
		Channel:            in.Channel,
		RedirectAfterLogin: in.RedirectAfterLogin,
	})
	if err != nil {
		return ports.StartOidcLoginOutput{}, err
	}
	return ports.StartOidcLoginOutput{AuthorizationURL: res.AuthorizationUrl, State: res.State}, nil
}

func (c *IdentityClient) CompleteOidcLogin(ctx context.Context, in ports.CompleteOidcLoginInput) (ports.CompleteOidcLoginOutput, error) {
	ctx2, cancel := context.WithTimeout(ctx, c.timeout)
	defer cancel()

	res, err := c.oidc.CompleteOidcLogin(ctx2, &identityv1.CompleteOidcLoginRequest{
		Code:      in.Code,
		State:     in.State,
		Ip:        in.IP,
		UserAgent: in.UserAgent,
		Channel:   in.Channel,
	})
	if err != nil {
		return ports.CompleteOidcLoginOutput{}, err
	}

	u := ports.OidcUser{}
	if res.User != nil {
		u = ports.OidcUser{Name: res.User.Name, Email: res.User.Email, Roles: append([]string{}, res.User.Roles...)}
	}

	return ports.CompleteOidcLoginOutput{
		IdentityID:         res.IdentityId,
		SubjectIDOidc:      res.SubjectIdOidc,
		Provider:           res.Provider,
		User:               u,
		SessionID:          res.SessionId,
		SessionExpiresIn:   res.SessionExpiresIn,
		RedirectAfterLogin: res.RedirectAfterLogin,
		MfaRequired:        res.MfaRequired,
		MfaVerified:        res.MfaVerified,
	}, nil
}

func (c *IdentityClient) RefreshSession(ctx context.Context, in ports.RefreshSessionInput) (ports.RefreshSessionOutput, error) {
	ctx2, cancel := context.WithTimeout(ctx, c.timeout)
	defer cancel()

	res, err := c.oidc.RefreshSession(ctx2, &identityv1.RefreshSessionRequest{
		SessionId: in.SessionID,
		Ip:        in.IP,
		UserAgent: in.UserAgent,
	})
	if err != nil {
		return ports.RefreshSessionOutput{}, err
	}
	return ports.RefreshSessionOutput{SessionID: res.SessionId, SessionExpiresIn: res.SessionExpiresIn}, nil
}

func (c *IdentityClient) LogoutSession(ctx context.Context, in ports.LogoutSessionInput) (ports.LogoutSessionOutput, error) {
	ctx2, cancel := context.WithTimeout(ctx, c.timeout)
	defer cancel()

	res, err := c.oidc.LogoutSession(ctx2, &identityv1.LogoutSessionRequest{SessionId: in.SessionID})
	if err != nil {
		return ports.LogoutSessionOutput{}, err
	}
	return ports.LogoutSessionOutput{Success: res.Success}, nil
}

func (c *IdentityClient) GetSessionInfo(ctx context.Context, in ports.GetSessionInfoInput) (ports.GetSessionInfoOutput, error) {
	ctx2, cancel := context.WithTimeout(ctx, c.timeout)
	defer cancel()

	res, err := c.oidc.GetSessionInfo(ctx2, &identityv1.GetSessionInfoRequest{
		SessionId: in.SessionID,
		Ip:        in.IP,
		UserAgent: in.UserAgent,
	})
	if err != nil {
		return ports.GetSessionInfoOutput{}, err
	}

	u := ports.OidcUser{}
	if res.User != nil {
		u = ports.OidcUser{Name: res.User.Name, Email: res.User.Email, Roles: append([]string{}, res.User.Roles...)}
	}

	return ports.GetSessionInfoOutput{
		IdentityID:       res.IdentityId,
		SubjectIDOidc:    res.SubjectIdOidc,
		Provider:         res.Provider,
		User:             u,
		CustomerID:       res.CustomerId,
		UserStatus:       res.UserStatus,
		SessionExpiresIn: res.SessionExpiresIn,
		MfaRequired:      res.MfaRequired,
		MfaVerified:      res.MfaVerified,
	}, nil
}

func (c *IdentityClient) StartRegistration(ctx context.Context, in ports.StartRegistrationInput) (ports.StartRegistrationOutput, error) {
	ctx2, cancel := context.WithTimeout(ctx, 8*time.Second) // KYC suele ser más pesado
	defer cancel()

	req := &identityv1.StartRegistrationRequest{
		Channel:             in.Channel,
		NationalId:          in.NationalID,
		NationalIdIssueDate: in.NationalIDIssueDate,
		FingerprintCode:     in.FingerprintCode,
		IdDocumentFront:     in.IdDocumentFront,
		Selfie:              in.Selfie,
		MonthlyIncome:       in.MonthlyIncome,
		Email:               in.Email,
		Phone:               in.Phone,
		OccupationType:      mapOccupation(in.OccupationType),
	}
	res, err := c.onboarding.StartRegistration(ctx2, req)
	if err != nil {
		return ports.StartRegistrationOutput{}, err
	}

	created := ""
	if res.CreatedAt != nil {
		created = res.CreatedAt.AsTime().Format(time.RFC3339)
	}
	return ports.StartRegistrationOutput{
		RegistrationID:   res.RegistrationId,
		State:            res.State,
		CreatedAtRFC3339: created,
	}, nil
}

func (c *IdentityClient) BeginWebauthnRegistration(ctx context.Context, in ports.BeginWebauthnRegistrationInput) (ports.BeginWebauthnRegistrationOutput, error) {
	ctx2, cancel := context.WithTimeout(ctx, c.timeout)
	defer cancel()

	res, err := c.webauthn.BeginWebauthnRegistration(ctx2, &identityv1.BeginWebauthnRegistrationRequest{
		IdentityId: in.IdentityID,
		DeviceName: in.DeviceName,
	})
	if err != nil {
		return ports.BeginWebauthnRegistrationOutput{}, err
	}
	return ports.BeginWebauthnRegistrationOutput{
		RequestID:   res.RequestId,
		OptionsJSON: res.OptionsJson,
	}, nil
}

func (c *IdentityClient) FinishWebauthnRegistration(ctx context.Context, in ports.FinishWebauthnRegistrationInput) (ports.FinishWebauthnRegistrationOutput, error) {
	ctx2, cancel := context.WithTimeout(ctx, c.timeout)
	defer cancel()

	res, err := c.webauthn.FinishWebauthnRegistration(ctx2, &identityv1.FinishWebauthnRegistrationRequest{
		RequestId:      in.RequestID,
		CredentialJson: in.CredentialJSON,
	})
	if err != nil {
		return ports.FinishWebauthnRegistrationOutput{}, err
	}
	return ports.FinishWebauthnRegistrationOutput{Success: res.Success}, nil
}

func (c *IdentityClient) BeginWebauthnAssertion(ctx context.Context, in ports.BeginWebauthnAssertionInput) (ports.BeginWebauthnAssertionOutput, error) {
	ctx2, cancel := context.WithTimeout(ctx, c.timeout)
	defer cancel()

	res, err := c.webauthn.BeginWebauthnAssertion(ctx2, &identityv1.BeginWebauthnAssertionRequest{
		IdentityId: in.IdentityID,
		SessionId:  in.SessionID,
	})
	if err != nil {
		return ports.BeginWebauthnAssertionOutput{}, err
	}
	return ports.BeginWebauthnAssertionOutput{
		RequestID:   res.RequestId,
		OptionsJSON: res.OptionsJson,
	}, nil
}

func (c *IdentityClient) FinishWebauthnAssertion(ctx context.Context, in ports.FinishWebauthnAssertionInput) (ports.FinishWebauthnAssertionOutput, error) {
	ctx2, cancel := context.WithTimeout(ctx, c.timeout)
	defer cancel()

	res, err := c.webauthn.FinishWebauthnAssertion(ctx2, &identityv1.FinishWebauthnAssertionRequest{
		SessionId:      in.SessionID,
		RequestId:      in.RequestID,
		CredentialJson: in.CredentialJSON,
	})
	if err != nil {
		return ports.FinishWebauthnAssertionOutput{}, err
	}
	return ports.FinishWebauthnAssertionOutput{Success: res.Success}, nil
}

func mapOccupation(s string) identityv1.OccupationType {
	s = strings.ToLower(strings.TrimSpace(s))
	switch s {
	case "student":
		return identityv1.OccupationType_OCCUPATION_TYPE_STUDENT
	case "employee":
		return identityv1.OccupationType_OCCUPATION_TYPE_EMPLOYEE
	case "self_employed":
		return identityv1.OccupationType_OCCUPATION_TYPE_SELF_EMPLOYED
	case "unemployed":
		return identityv1.OccupationType_OCCUPATION_TYPE_UNEMPLOYED
	case "retired":
		return identityv1.OccupationType_OCCUPATION_TYPE_RETIRED
	default:
		return identityv1.OccupationType_OCCUPATION_TYPE_UNSPECIFIED
	}
}

// evitar import no usado si tu IDE se queja en algún refactor
var _ = timestamppb.Now
