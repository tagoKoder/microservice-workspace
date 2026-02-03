package com.tagokoder.identity.infra.out.cache;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.tagokoder.identity.domain.port.out.OidcStateRepositoryPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisOidcStateRepository implements OidcStateRepositoryPort {

    private final StringRedisTemplate redis;
    private final JsonMapper  om = new JsonMapper();

    public RedisOidcStateRepository(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void saveState(String state, String codeVerifier, String redirectAfterLogin, String nonce, Duration ttl) {
        try {
            var payload = new Payload(codeVerifier, redirectAfterLogin, nonce);
            redis.opsForValue().set(key(state), om.writeValueAsString(payload), ttl);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot persist OIDC state", e);
        }
    }

    @Override
    public OidcState loadAndRemove(String state) {
        String k = key(state);
        String value = redis.opsForValue().get(k);
        if (value == null) return null;

        redis.delete(k);

        try {
            var p = om.readValue(value, Payload.class);
            return new OidcState(p.codeVerifier, p.redirectAfterLogin, p.nonce);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid OIDC state payload", e);
        }
    }

    private String key(String state) { return "oidc:state:" + state; }

    record Payload(String codeVerifier, String redirectAfterLogin, String nonce) {}
}
