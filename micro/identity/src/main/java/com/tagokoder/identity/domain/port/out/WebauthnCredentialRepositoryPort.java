package com.tagokoder.identity.domain.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebauthnCredentialRepositoryPort {

    record WebauthnCredential(
        UUID id,
        UUID identityId,
        String credentialId,
        byte[] publicKeyCose,
        long signCount,
        UUID aaguid,
        String transports,
        String name,
        boolean enabled,
        Instant createdAt,
        Instant lastUsedAt
    ) {}

    List<WebauthnCredential> findByIdentityId(UUID identityId);

    Optional<WebauthnCredential> findByCredentialId(String credentialId);

    WebauthnCredential save(WebauthnCredential c);

    long countEnabledByIdentityId(UUID identityId);

    void updateSignCountAndLastUsed(UUID id, long signCount, Instant lastUsedAt);
}
