package com.tagokoder.identity.infra.out.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tagokoder.identity.infra.out.persistence.jpa.entity.RegistrationIntentEntity;

import java.util.UUID;

public interface SpringDataRegistrationIntentJpa extends JpaRepository<RegistrationIntentEntity, UUID> {
}
