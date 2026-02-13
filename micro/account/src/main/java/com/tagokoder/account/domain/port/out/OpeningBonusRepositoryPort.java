package com.tagokoder.account.domain.port.out;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface OpeningBonusRepositoryPort {

    record BonusGrant(
            String idempotencyKey,
            UUID accountId,
            String journalId,
            BigDecimal amount,
            String currency
    ) {}

    Optional<BonusGrant> findByKey(String idempotencyKey);

    /**
     * Inserta el grant si NO existe (unique por idempotencyKey).
     * @return true si insertó (primer apply), false si ya existía.
     */
    boolean tryInsert(BonusGrant grant);
}