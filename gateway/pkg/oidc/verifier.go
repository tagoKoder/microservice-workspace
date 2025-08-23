package oidc

import (
	"context"

	gooidc "github.com/coreos/go-oidc/v3/oidc"
)

// Options por si en el futuro quisieras validar aud/clientID, clock skew, etc.
type OidcOptions struct {
	SkipClientIDCheck bool
}

type Verifier struct {
	verifier *gooidc.IDTokenVerifier
}

// NewVerifier carga el .well-known y prepara el verificador (SkipClientIDCheck
// porque en Authentik el access_token suele ser JWT con 'aud' != client_id).
func NewVerifier(ctx context.Context, issuer string, opts OidcOptions) (*Verifier, error) {
	p, err := gooidc.NewProvider(ctx, issuer)
	if err != nil {
		return nil, err
	}
	cfg := &gooidc.Config{SkipClientIDCheck: opts.SkipClientIDCheck}
	return &Verifier{verifier: p.Verifier(cfg)}, nil
}

// VerifyAccessToken verifica firma/exp/iss del token (no extrae el header).
func (v *Verifier) VerifyAccessToken(ctx context.Context, raw string) error {
	_, err := v.verifier.Verify(ctx, raw)
	return err
}
