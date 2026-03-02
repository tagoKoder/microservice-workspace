package com.tagokoder.account.infra.out.persistence.jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tagokoder.account.infra.out.persistence.jpa.entity.OpeningBonusGrantEntity;

import jakarta.transaction.Transactional;

@Repository
public interface SpringDataOpeningBonusGrantJpa extends JpaRepository<OpeningBonusGrantEntity, UUID> {

  Optional<OpeningBonusGrantEntity> findByIdempotencyKey(String idempotencyKey);

  @Modifying
  @Transactional
  @Query(value = """
    UPDATE account_opening_bonus_grants
       SET account_id = :accountId
     WHERE idempotency_key = :key
       AND account_id IS NULL
  """, nativeQuery = true)
  int attachAccountIfEmpty(@Param("key") String key, @Param("accountId") UUID accountId);

  @Modifying
  @Transactional
  @Query(value = """
    UPDATE account_opening_bonus_grants
       SET status = 'COMPLETED',
           account_id = COALESCE(account_id, :accountId),
           journal_id = :journalId,
           locked_at = NULL,
           locked_by = NULL,
           updated_at = now()
     WHERE idempotency_key = :key
       AND status = 'PENDING'
  """, nativeQuery = true)
  int completeIfPending(@Param("key") String key, @Param("accountId") UUID accountId, @Param("journalId") String journalId);
}