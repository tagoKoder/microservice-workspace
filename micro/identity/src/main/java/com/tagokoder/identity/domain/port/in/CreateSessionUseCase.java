package com.tagokoder.identity.domain.port.in;

import java.util.UUID;

import com.tagokoder.identity.domain.model.Identity;

public interface CreateSessionUseCase {
    CreatedSession createSession(Identity identity, String refreshToken, String ip, String ua);

    record CreatedSession(UUID sessionId, long expiresInSeconds) {}
}
