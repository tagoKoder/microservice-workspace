package com.tagokoder.account.infra.out.persistence.jpa.adapter;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.tagokoder.account.domain.port.out.AccountBalanceRepositoryPort;
import com.tagokoder.account.infra.out.persistence.jpa.SpringDataAccountBalanceJpa;
import com.tagokoder.account.infra.out.persistence.jpa.entity.AccountBalanceEntity;

@Component
public class AccountBalanceRepositoryAdapter implements AccountBalanceRepositoryPort {

    private final SpringDataAccountBalanceJpa jpa;

    public AccountBalanceRepositoryAdapter(SpringDataAccountBalanceJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<BalancesRow> findByAccountId(UUID accountId) {
        return jpa.findByAccountId(accountId).map(e ->
                new BalancesRow(
                        e.getLedger().doubleValue(),
                        e.getAvailable().doubleValue(),
                        e.getHold().doubleValue()
                ));
    }

    @Override
    public void initZero(UUID accountId) {
        AccountBalanceEntity e = new AccountBalanceEntity();
        e.setAccountId(accountId);
        e.setLedger(BigDecimal.ZERO);
        e.setAvailable(BigDecimal.ZERO);
        e.setHold(BigDecimal.ZERO);
        jpa.save(e);
    }

    @Override
    public Double incrementHold(UUID accountId, Double amount) {
        var _ = jpa.findByAccountId(accountId).orElseThrow(() -> new IllegalArgumentException("balances not found"));
        jpa.addHold(accountId, BigDecimal.valueOf(amount));
        var updated = jpa.findByAccountId(accountId).orElseThrow();
        return updated.getHold().doubleValue();
    }

    @Override
    public Double decrementHold(UUID accountId, Double amount) {
        var _ = jpa.findByAccountId(accountId).orElseThrow(() -> new IllegalArgumentException("balances not found"));
        jpa.subHold(accountId, BigDecimal.valueOf(amount));
        var updated = jpa.findByAccountId(accountId).orElseThrow();
        return updated.getHold().doubleValue();
    }

}
