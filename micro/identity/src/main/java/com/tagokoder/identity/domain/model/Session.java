package com.tagokoder.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Session {

    private UUID sessionId;
    private UUID identityId;

    // Hash del refresh token (HMAC/SHA-256)
    private String refreshTokenHash;

    // Refresh token cifrado (AES-GCM)
    private String refreshTokenEnc;

    private Instant expiresAt;
    private Instant createdAt;
    private Instant revokedAt;
    private Instant absoluteExpiresAt;
    private UUID rotatedToSessionId;
    private String accessTokenEnc;
    private Instant accessTokenExpiresAt;
    private String ip;
    private String ua;

}
