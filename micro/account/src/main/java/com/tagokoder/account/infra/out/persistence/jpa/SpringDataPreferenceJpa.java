package com.tagokoder.account.infra.out.persistence.jpa;

import com.tagokoder.account.infra.out.persistence.jpa.entity.PreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataPreferenceJpa extends JpaRepository<PreferenceEntity, Long> {
    Optional<PreferenceEntity> findFirstByCustomerId(UUID customerId);
}
