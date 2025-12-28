package com.tagokoder.identity.infra.out.cache;


import com.tagokoder.identity.domain.port.out.OidcStateRepositoryPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisOidcStateRepository implements OidcStateRepositoryPort {

    private final StringRedisTemplate redis;

    public RedisOidcStateRepository(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void saveState(String state, String codeVerifier, String redirectAfterLogin, Duration ttl) {
        String value = codeVerifier + "|" + redirectAfterLogin;
        redis.opsForValue().set(key(state), value, ttl);
    }

    @Override
    public OidcState loadAndRemove(String state) {
        String k = key(state);
        String value = redis.opsForValue().get(k);
        if (value == null) return null;
        redis.delete(k);
        String[] parts = value.split("\\|", 2);
        String verifier = parts[0];
        String redirect = parts.length > 1 ? parts[1] : "/";
        return new OidcState(verifier, redirect);
    }

    private String key(String state) {
        return "oidc:state:" + state;
    }
}