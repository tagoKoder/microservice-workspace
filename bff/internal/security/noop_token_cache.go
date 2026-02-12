// bff/internal/security/noop_token_cache.go
package security

import (
	"context"
	"time"
)

type NoopTokenCache struct{}

func (NoopTokenCache) Get(ctx context.Context, sid string) (string, time.Time, bool) {
	return "", time.Time{}, false
}
func (NoopTokenCache) Set(ctx context.Context, sid string, tok string, expAt time.Time) error {
	return nil
}
func (NoopTokenCache) Del(ctx context.Context, sid string) error { return nil }
