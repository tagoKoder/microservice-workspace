// bff\internal\security\token_cache.go
package security

import (
	"context"
	"encoding/json"
	"time"

	"github.com/redis/go-redis/v9"
)

type TokenCache interface {
	Get(ctx context.Context, sid string) (tok string, expAt time.Time, ok bool)
	Set(ctx context.Context, sid string, tok string, expAt time.Time) error
	Del(ctx context.Context, sid string) error
}

type redisTokenCache struct {
	rdb  *redis.Client
	pfx  string
	skew time.Duration
}

type tokenPayload struct {
	Token string `json:"t"`
	ExpAt int64  `json:"e"` // unix seconds
}

func NewRedisTokenCacheWithOptions(rdb *redis.Client, prefix string, skew time.Duration) TokenCache {
	if prefix == "" {
		prefix = "bff:tok:"
	}
	if skew <= 0 {
		skew = 30 * time.Second
	}
	return &redisTokenCache{
		rdb:  rdb,
		pfx:  prefix,
		skew: skew,
	}
}

func NewRedisTokenCache(rdb *redis.Client) TokenCache {
	return &redisTokenCache{
		rdb:  rdb,
		pfx:  "bff:tok:",
		skew: 30 * time.Second,
	}
}

func (c *redisTokenCache) key(sid string) string { return c.pfx + sid }

func (c *redisTokenCache) Get(ctx context.Context, sid string) (string, time.Time, bool) {
	b, err := c.rdb.Get(ctx, c.key(sid)).Bytes()
	if err != nil {
		return "", time.Time{}, false
	}
	var p tokenPayload
	if json.Unmarshal(b, &p) != nil || p.Token == "" || p.ExpAt == 0 {
		return "", time.Time{}, false
	}
	exp := time.Unix(p.ExpAt, 0)
	// válido con skew
	if time.Now().Add(c.skew).After(exp) {
		return "", time.Time{}, false
	}
	return p.Token, exp, true
}

func (c *redisTokenCache) Set(ctx context.Context, sid string, tok string, expAt time.Time) error {
	if sid == "" || tok == "" || expAt.IsZero() {
		return nil
	}
	ttl := time.Until(expAt) - c.skew
	if ttl <= 0 {
		return nil
	}
	p := tokenPayload{Token: tok, ExpAt: expAt.Unix()}
	b, _ := json.Marshal(p)
	return c.rdb.SetEx(ctx, c.key(sid), b, ttl).Err()
}

func (c *redisTokenCache) Del(ctx context.Context, sid string) error {
	if sid == "" {
		return nil
	}
	return c.rdb.Del(ctx, c.key(sid)).Err()
}
