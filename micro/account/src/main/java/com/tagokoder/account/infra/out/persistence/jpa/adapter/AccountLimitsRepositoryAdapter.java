package com.tagokoder.account.infra.out.persistence.jpa.adapter;

import com.tagokoder.account.domain.port.out.AccountLimitsRepositoryPort;
import com.tagokoder.account.infra.out.persistence.jpa.SpringDataAccountLimitJpa;
import com.tagokoder.account.infra.out.persistence.jpa.entity.AccountEntity;
import com.tagokoder.account.infra.out.persistence.jpa.entity.AccountLimitEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Component
public class AccountLimitsRepositoryAdapter implements AccountLimitsRepositoryPort {

    private final SpringDataAccountLimitJpa jpa;
    @PersistenceContext
    private EntityManager em;

    public AccountLimitsRepositoryAdapter(SpringDataAccountLimitJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<LimitsRow> findByAccountId(UUID accountId) {
        return jpa.findByAccountId(accountId).map(e -> new LimitsRow(
                e.getDailyOut(),
                e.getDailyIn()
        ));
    }

    @Override
    public LimitsRow patch(UUID accountId, BigDecimal dailyOut, BigDecimal dailyIn) {
        AccountLimitEntity e = jpa.findByAccountId(accountId).orElseGet(() -> {
            AccountLimitEntity x = new AccountLimitEntity();
            x.setAccount(em.getReference(AccountEntity.class, accountId));
            //x.setAccountId(accountId);
            x.setDailyOut(BigDecimal.ZERO);
            x.setDailyIn(BigDecimal.ZERO);
            return x;
        });

        if (dailyOut != null) e.setDailyOut(dailyOut);
        if (dailyIn != null) e.setDailyIn(dailyIn);

        AccountLimitEntity saved = jpa.save(e);
        return new LimitsRow(saved.getDailyOut(), saved.getDailyIn());
    }
}
