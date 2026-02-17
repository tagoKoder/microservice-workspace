package com.tagokoder.account.infra.out.persistence.jpa;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import com.tagokoder.account.infra.out.persistence.jpa.entity.OpeningBonusGrantEntity;

public interface SpringDataOpeningBonusGrantJpa extends JpaRepository<OpeningBonusGrantEntity, String> {

    Optional<OpeningBonusGrantEntity> findByIdempotencyKey(String idempotencyKey);

    @Modifying
    @Query(value = """
        insert into account_opening_bonus_grants(idempotency_key, account_id, journal_id, amount, currency)
        values (:key, :accountId, :journalId, :amount, :currency)
        on conflict (idempotency_key) do nothing
    """, nativeQuery = true)
    int insertIfAbsent(
            @Param("key") String key,
            @Param("accountId") UUID accountId,
            @Param("journalId") String journalId,
            @Param("amount") BigDecimal amount,
            @Param("currency") String currency
    );
}