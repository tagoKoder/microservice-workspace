package com.tagokoder.identity.infra.out.persistence.jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tagokoder.identity.infra.out.persistence.jpa.entity.RegistrationIntentEntity;

public interface SpringDataRegistrationIntentJpa extends JpaRepository<RegistrationIntentEntity, UUID> {
    @EntityGraph(attributePaths = {"kycObjects"})
    Optional<RegistrationIntentEntity> findTopByEmailIgnoreCaseOrderByUpdatedAtDesc(String email);
}
