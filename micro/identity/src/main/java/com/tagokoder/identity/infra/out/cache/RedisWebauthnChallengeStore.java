package com.tagokoder.identity.infra.out.cache;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.tagokoder.identity.domain.port.out.WebauthnChallengeStorePort;

@Component
public class RedisWebauthnChallengeStore implements WebauthnChallengeStorePort {

    private final StringRedisTemplate redis;

    public RedisWebauthnChallengeStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void save(String requestId, String json, Duration ttl) {
        redis.opsForValue().set(key(requestId), json, ttl);
    }

    @Override
    public String loadAndRemove(String requestId) {
        String k = key(requestId);
        String v = redis.opsForValue().get(k);
        if (v != null) redis.delete(k);
        return v;
    }

    private String key(String requestId) {
        return "webauthn:req:" + requestId;
    }
}
