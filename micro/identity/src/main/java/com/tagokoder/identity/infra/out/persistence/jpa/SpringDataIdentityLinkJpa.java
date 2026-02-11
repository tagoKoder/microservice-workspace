package com.tagokoder.identity.infra.out.persistence.jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tagokoder.identity.infra.out.persistence.jpa.entity.IdentityLinkEntity;

public interface SpringDataIdentityLinkJpa extends JpaRepository<IdentityLinkEntity, UUID> {
  Optional<IdentityLinkEntity> findByIdentityId(UUID identityId);
  Optional<IdentityLinkEntity> findByCustomerId(String customerId);
}
