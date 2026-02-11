package com.tagokoder.identity.domain.port.in;

import java.util.UUID;

public interface GetSessionInfoUseCase {
    SessionInfo get(UUID sessionId, String ip, String userAgent);

    record SessionInfo(
            UUID identityId,
            String subjectIdOidc,
            String provider,
            String userStatus,
            long expiresInSeconds,
            String customerId,
            String accessToken,
            long accessTokenExpiresIn
    ) {}
}
