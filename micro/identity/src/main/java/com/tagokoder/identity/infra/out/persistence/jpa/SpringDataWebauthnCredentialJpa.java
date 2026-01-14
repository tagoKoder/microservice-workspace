package com.tagokoder.identity.infra.out.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tagokoder.identity.infra.out.persistence.jpa.entity.WebauthnCredentialEntity;

public interface SpringDataWebauthnCredentialJpa extends JpaRepository<WebauthnCredentialEntity, UUID> {

    List<WebauthnCredentialEntity> findByIdentity_Id(UUID identityId);

    Optional<WebauthnCredentialEntity> findByCredentialId(String credentialId);

    long countByIdentity_IdAndEnabledTrue(UUID identityId);
}
