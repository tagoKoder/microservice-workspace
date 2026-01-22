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
    return jpa.findById(accountId).map(e ->
      new BalancesRow(e.getLedger(), e.getAvailable(), e.getHold())
    );
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
  public BigDecimal incrementHold(UUID accountId, BigDecimal amount) {
    int updated = jpa.reserveHoldAtomic(accountId, amount);
    if (updated != 1) throw new IllegalArgumentException("insufficient available or balances not found");
    return jpa.findById(accountId).orElseThrow().getHold();
  }

  @Override
  public BigDecimal decrementHold(UUID accountId, BigDecimal amount) {
    int updated = jpa.releaseHoldAtomic(accountId, amount);
    if (updated != 1) throw new IllegalArgumentException("insufficient hold or balances not found");
    return jpa.findById(accountId).orElseThrow().getHold();
  }

}
