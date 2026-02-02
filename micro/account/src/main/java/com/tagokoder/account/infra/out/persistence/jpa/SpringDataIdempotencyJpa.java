package com.tagokoder.account.infra.out.persistence.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tagokoder.account.infra.out.persistence.jpa.entity.IdempotencyRecordEntity;

public interface SpringDataIdempotencyJpa extends JpaRepository<IdempotencyRecordEntity, java.util.UUID> {
  Optional<IdempotencyRecordEntity> findByIdempotencyKey(String idempotencyKey);
}
