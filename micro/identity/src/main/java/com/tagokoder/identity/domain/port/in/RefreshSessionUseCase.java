package com.tagokoder.identity.domain.port.in;

import java.util.UUID;

public interface RefreshSessionUseCase {
    RefreshedSession refresh(UUID sessionId, String ip, String ua);

    record RefreshedSession(UUID sessionId, long expiresInSeconds) {}
}
