package com.tagokoder.account.infra.out.persistence.jpa.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tagokoder.account.domain.port.out.OpeningBonusRepositoryPort;
import com.tagokoder.account.infra.out.persistence.jpa.SpringDataOpeningBonusGrantJpa;

@Component
public class OpeningBonusRepositoryAdapter implements OpeningBonusRepositoryPort {

    private final SpringDataOpeningBonusGrantJpa jpa;

    public OpeningBonusRepositoryAdapter(SpringDataOpeningBonusGrantJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<BonusGrant> findByKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return Optional.empty();

        return jpa.findByIdempotencyKey(idempotencyKey).map(e ->
                new BonusGrant(
                        e.getIdempotencyKey(),
                        e.getAccountId(),
                        e.getJournalId(),
                        e.getAmount(),
                        e.getCurrency()
                )
        );
    }

    @Override
    @Transactional
    public boolean tryInsert(BonusGrant grant) {
        if (grant == null) throw new IllegalArgumentException("grant is required");
        int inserted = jpa.insertIfAbsent(
                grant.idempotencyKey(),
                grant.accountId(),
                grant.journalId(),
                grant.amount(),
                grant.currency()
        );
        return inserted == 1;
    }
}