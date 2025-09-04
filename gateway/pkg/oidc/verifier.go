// gateway/pkg/oidc/multi.go
package oidc

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"strings"
	"sync"
	"time"

	keyfunc "github.com/MicahParks/keyfunc/v3"
	"github.com/golang-jwt/jwt/v5"
)

type OidcOptions struct {
	Audience        string            // aud global opcional
	AudiencesPerIss map[string]string // aud por issuer (opcional)
	HTTPClient      *http.Client      // opcional
}

type issuerCfg struct {
	Issuer  string `json:"issuer"`
	JWKSURI string `json:"jwks_uri"`
}

type Verifier struct {
	mu     sync.RWMutex
	keyset map[string]keyfunc.Keyfunc // issuer -> keyfunc
	opts   OidcOptions
	client *http.Client
}

func NewMultiIssuerVerifier(ctx context.Context, issuers []string, opts OidcOptions) (*Verifier, error) {
	if len(issuers) == 0 {
		return nil, errors.New("no issuers configured")
	}
	v := &Verifier{
		keyset: make(map[string]keyfunc.Keyfunc),
		opts:   opts,
		client: opts.HTTPClient,
	}
	if v.client == nil {
		v.client = &http.Client{Timeout: 10 * time.Second}
	}
	for _, iss := range issuers {
		if err := v.addIssuer(ctx, strings.TrimRight(iss, "/")); err != nil {
			return nil, fmt.Errorf("issuer %s: %w", iss, err)
		}
	}
	return v, nil
}

func (v *Verifier) addIssuer(ctx context.Context, issuer string) error {
	wellKnown := issuer + "/.well-known/openid-configuration"
	req, _ := http.NewRequestWithContext(ctx, "GET", wellKnown, nil)
	res, err := v.client.Do(req)
	if err != nil {
		return err
	}
	defer res.Body.Close()
	if res.StatusCode != 200 {
		return fmt.Errorf("well-known %s -> %d", wellKnown, res.StatusCode)
	}
	var cfg issuerCfg
	if err := json.NewDecoder(res.Body).Decode(&cfg); err != nil {
		return err
	}
	if cfg.Issuer == "" || cfg.JWKSURI == "" {
		return fmt.Errorf("missing issuer/jwks_uri for %s", issuer)
	}

	kf, err := keyfunc.NewDefaultCtx(ctx, []string{cfg.JWKSURI}) // v3
	if err != nil {
		return err
	}
	v.mu.Lock()
	v.keyset[cfg.Issuer] = kf
	v.mu.Unlock()
	return nil
}

func (v *Verifier) expectedAudienceFor(iss string) (string, bool) {
	if v.opts.AudiencesPerIss != nil {
		if aud := v.opts.AudiencesPerIss[iss]; aud != "" {
			return aud, true
		}
	}
	if v.opts.Audience != "" {
		return v.opts.Audience, true
	}
	return "", false
}

// Verifica firma/exp/iss y (opcionalmente) aud. Devuelve claims si es válido.
func (v *Verifier) VerifyAccessToken(ctx context.Context, raw string) (jwt.MapClaims, error) {
	// Leer 'iss' sin validar firma (solo para saber qué JWKS usar).
	tok, _, err := new(jwt.Parser).ParseUnverified(raw, jwt.MapClaims{})
	if err != nil {
		return nil, fmt.Errorf("parse (unverified): %w", err)
	}
	unver := tok.Claims.(jwt.MapClaims)
	iss, _ := unver["iss"].(string)
	if iss == "" {
		return nil, errors.New("missing iss")
	}

	v.mu.RLock()
	kf := v.keyset[iss]
	v.mu.RUnlock()
	if kf == nil {
		return nil, fmt.Errorf("issuer not allowed: %s", iss)
	}

	// Opciones de validación (v5)
	opts := []jwt.ParserOption{
		jwt.WithIssuer(iss),
		jwt.WithLeeway(30 * time.Second),
		jwt.WithValidMethods([]string{
			"RS256", "RS384", "RS512",
			"ES256", "ES384", "ES512",
			"PS256", "PS384", "PS512",
			"EdDSA",
		}),
	}
	if aud, ok := v.expectedAudienceFor(iss); ok {
		opts = append(opts, jwt.WithAudience(aud))
	}

	validTok, err := jwt.Parse(raw, kf.Keyfunc, opts...)
	if err != nil || !validTok.Valid {
		return nil, fmt.Errorf("jwt invalid: %w", err)
	}
	return validTok.Claims.(jwt.MapClaims), nil
}
