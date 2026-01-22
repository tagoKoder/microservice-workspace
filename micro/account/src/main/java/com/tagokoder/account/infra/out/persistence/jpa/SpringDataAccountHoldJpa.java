package com.tagokoder.account.infra.out.persistence.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tagokoder.account.infra.out.persistence.jpa.entity.AccountHoldEntity;
import com.tagokoder.account.infra.out.persistence.jpa.entity.AccountHoldId;

public interface SpringDataAccountHoldJpa extends JpaRepository<AccountHoldEntity, AccountHoldId> {
  Optional<AccountHoldEntity> findByIdempotencyKey(String idempotencyKey);
}
