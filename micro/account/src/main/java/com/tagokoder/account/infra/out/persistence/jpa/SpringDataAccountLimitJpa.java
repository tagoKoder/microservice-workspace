package com.tagokoder.account.infra.out.persistence.jpa;

import com.tagokoder.account.infra.out.persistence.jpa.entity.AccountLimitEntity;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAccountLimitJpa extends JpaRepository<AccountLimitEntity, UUID> {
    Optional<AccountLimitEntity> findByAccountId(UUID accountId);
}
