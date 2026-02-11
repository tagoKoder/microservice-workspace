// bff\internal\security\access_token_provider.go
package security

import (
	"context"
	"sync"
	"time"

	"github.com/tagoKoder/bff/internal/client/ports"
)

type AccessTokenProvider struct {
	mu       sync.Mutex
	cache    map[string]tokenEntry
	skew     time.Duration
	identity ports.IdentityPort
}

type tokenEntry struct {
	sessionID        string
	sessionExpiresAt time.Time

	accessToken string
	accessExp   time.Time
}

type EnsureResult struct {
	// lo que debes usar a partir de ahora
	SessionID        string
	SessionExpiresIn int64

	AccessToken          string
	AccessTokenExpiresIn int64
}

func NewAccessTokenProvider(identity ports.IdentityPort) *AccessTokenProvider {
	return &AccessTokenProvider{
		cache:    make(map[string]tokenEntry),
		skew:     30 * time.Second,
		identity: identity,
	}
}

// Ensure retorna token válido y maneja rotación de session_id.
// Si el token en cache está vigente (con skew), lo reutiliza.
// Caso contrario, llama Identity.RefreshSession.
func (p *AccessTokenProvider) Ensure(ctx context.Context, sessionID, ip, ua string) (EnsureResult, error) {
	now := time.Now()

	// hit cache
	p.mu.Lock()
	if e, ok := p.cache[sessionID]; ok {
		if e.accessToken != "" && now.Add(p.skew).Before(e.accessExp) {
			out := EnsureResult{
				SessionID:            e.sessionID,
				SessionExpiresIn:     int64(e.sessionExpiresAt.Sub(now).Seconds()),
				AccessToken:          e.accessToken,
				AccessTokenExpiresIn: int64(e.accessExp.Sub(now).Seconds()),
			}
			p.mu.Unlock()
			return out, nil
		}
	}
	p.mu.Unlock()

	// refresh
	res, err := p.identity.RefreshSession(ctx, ports.RefreshSessionInput{
		SessionID: sessionID,
		IP:        ip,
		UserAgent: ua,
	})
	if err != nil {
		return EnsureResult{}, err
	}

	newSid := res.SessionID
	sessExp := now.Add(time.Duration(res.SessionExpiresIn) * time.Second)
	atExp := now.Add(time.Duration(res.AccessTokenExpiresIn) * time.Second)

	p.mu.Lock()
	// guarda con la nueva sid (si rotó)
	p.cache[newSid] = tokenEntry{
		sessionID:        newSid,
		sessionExpiresAt: sessExp,
		accessToken:      res.AccessToken,
		accessExp:        atExp,
	}
	// limpia la vieja
	if newSid != sessionID {
		delete(p.cache, sessionID)
	}
	p.mu.Unlock()

	return EnsureResult{
		SessionID:            newSid,
		SessionExpiresIn:     res.SessionExpiresIn,
		AccessToken:          res.AccessToken,
		AccessTokenExpiresIn: res.AccessTokenExpiresIn,
	}, nil
}

func (p *AccessTokenProvider) Invalidate(sessionID string) {
	p.mu.Lock()
	delete(p.cache, sessionID)
	p.mu.Unlock()
}

func (p *AccessTokenProvider) Store(sessionID string, sessExp time.Time, accessToken string, accessExp time.Time) {
	p.mu.Lock()
	p.cache[sessionID] = tokenEntry{
		sessionID:        sessionID,
		sessionExpiresAt: sessExp,
		accessToken:      accessToken,
		accessExp:        accessExp,
	}
	p.mu.Unlock()
}
