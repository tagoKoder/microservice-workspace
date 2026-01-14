package com.tagokoder.identity.infra.out.persistence.jpa.adapter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tagokoder.identity.domain.port.out.WebauthnCredentialRepositoryPort;
import com.tagokoder.identity.infra.out.persistence.jpa.SpringDataIdentityJpa;
import com.tagokoder.identity.infra.out.persistence.jpa.SpringDataWebauthnCredentialJpa;
import com.tagokoder.identity.infra.out.persistence.jpa.entity.WebauthnCredentialEntity;

@Component
public class WebauthnCredentialRepositoryAdapter implements WebauthnCredentialRepositoryPort {

    private final SpringDataWebauthnCredentialJpa jpa;
    private final SpringDataIdentityJpa identityJpa;

    public WebauthnCredentialRepositoryAdapter(SpringDataWebauthnCredentialJpa jpa,
                                              SpringDataIdentityJpa identityJpa) {
        this.jpa = jpa;
        this.identityJpa = identityJpa;
    }

    @Override
    public List<WebauthnCredential> findByIdentityId(UUID identityId) {
        return jpa.findByIdentity_Id(identityId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<WebauthnCredential> findByCredentialId(String credentialId) {
        return jpa.findByCredentialId(credentialId).map(this::toDomain);
    }

    @Override
    public long countEnabledByIdentityId(UUID identityId) {
        return jpa.countByIdentity_IdAndEnabledTrue(identityId);
    }

    @Override
    @Transactional
    public WebauthnCredential save(WebauthnCredential c) {
        WebauthnCredentialEntity e = new WebauthnCredentialEntity();
        e.setId(c.id());
        e.setIdentity(identityJpa.getReferenceById(c.identityId()));
        e.setCredentialId(c.credentialId());
        e.setPublicKeyCose(c.publicKeyCose());
        e.setSignCount(c.signCount());
        e.setAaguid(c.aaguid());
        e.setTransports(c.transports());
        e.setName(c.name());
        e.setEnabled(c.enabled());
        e.setCreatedAt(c.createdAt());
        e.setLastUsedAt(c.lastUsedAt());

        WebauthnCredentialEntity saved = jpa.save(e);
        return toDomain(saved);
    }

    @Override
    @Transactional
    public void updateSignCountAndLastUsed(UUID id, long signCount, Instant lastUsedAt) {
        WebauthnCredentialEntity e = jpa.findById(id).orElseThrow();
        e.setSignCount(signCount);
        e.setLastUsedAt(lastUsedAt);
        jpa.save(e);
    }

    private WebauthnCredential toDomain(WebauthnCredentialEntity e) {
        return new WebauthnCredential(
            e.getId(),
            e.getIdentity().getId(),
            e.getCredentialId(),
            e.getPublicKeyCose(),
            e.getSignCount(),
            e.getAaguid(),
            e.getTransports(),
            e.getName(),
            e.isEnabled(),
            e.getCreatedAt(),
            e.getLastUsedAt()
        );
    }
}
