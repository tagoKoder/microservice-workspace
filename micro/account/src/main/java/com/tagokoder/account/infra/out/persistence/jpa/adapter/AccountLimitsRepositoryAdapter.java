package com.tagokoder.account.infra.out.persistence.jpa.adapter;

import com.tagokoder.account.domain.port.out.AccountLimitsRepositoryPort;
import com.tagokoder.account.infra.out.persistence.jpa.SpringDataAccountLimitJpa;
import com.tagokoder.account.infra.out.persistence.jpa.entity.AccountLimitEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Component
public class AccountLimitsRepositoryAdapter implements AccountLimitsRepositoryPort {

    private final SpringDataAccountLimitJpa jpa;

    public AccountLimitsRepositoryAdapter(SpringDataAccountLimitJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<LimitsRow> findByAccountId(UUID accountId) {
        return jpa.findByAccountId(accountId).map(e -> new LimitsRow(
                e.getDailyOut().doubleValue(),
                e.getDailyIn().doubleValue()
        ));
    }

    @Override
    public LimitsRow patch(UUID accountId, Double dailyOut, Double dailyIn) {
        AccountLimitEntity e = jpa.findByAccountId(accountId).orElseGet(() -> {
            AccountLimitEntity x = new AccountLimitEntity();
            x.setAccountId(accountId);
            x.setDailyOut(BigDecimal.ZERO);
            x.setDailyIn(BigDecimal.ZERO);
            return x;
        });

        if (dailyOut != null) e.setDailyOut(BigDecimal.valueOf(dailyOut));
        if (dailyIn != null) e.setDailyIn(BigDecimal.valueOf(dailyIn));

        AccountLimitEntity saved = jpa.save(e);
        return new LimitsRow(saved.getDailyOut().doubleValue(), saved.getDailyIn().doubleValue());
    }
}
