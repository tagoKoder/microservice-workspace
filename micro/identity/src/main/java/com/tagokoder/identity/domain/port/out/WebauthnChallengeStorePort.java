package com.tagokoder.identity.domain.port.out;

import java.time.Duration;

public interface WebauthnChallengeStorePort {

    void save(String requestId, String json, Duration ttl);

    String loadAndRemove(String requestId);
}
