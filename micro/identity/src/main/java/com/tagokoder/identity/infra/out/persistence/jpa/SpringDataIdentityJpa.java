package com.tagokoder.identity.infra.out.persistence.jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tagokoder.identity.infra.out.persistence.jpa.entity.IdentityEntity;

public interface SpringDataIdentityJpa extends JpaRepository<IdentityEntity, UUID> {
    Optional<IdentityEntity> findBySubjectIdOidcAndProvider(String subjectIdOidc, String provider);
}
