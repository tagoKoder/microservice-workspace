package com.tagokoder.identity.domain.port.in;

import java.util.UUID;

public interface LogoutSessionUseCase {
    void logout(UUID sessionId);
}
