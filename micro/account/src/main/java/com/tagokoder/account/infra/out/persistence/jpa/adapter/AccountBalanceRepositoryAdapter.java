package com.tagokoder.account.infra.out.persistence.jpa.adapter;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tagokoder.account.domain.port.out.AccountBalanceRepositoryPort;
import com.tagokoder.account.infra.out.persistence.jpa.SpringDataAccountBalanceJpa;
import com.tagokoder.account.infra.out.persistence.jpa.entity.AccountBalanceEntity;
import com.tagokoder.account.infra.out.persistence.jpa.entity.AccountEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Component
public class AccountBalanceRepositoryAdapter implements AccountBalanceRepositoryPort {

    private final SpringDataAccountBalanceJpa jpa;
    @PersistenceContext
    private EntityManager em;

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
    @Transactional
    public void init(UUID accountId, BigDecimal ledger, BigDecimal available, BigDecimal hold) {
        if (accountId == null) throw new IllegalArgumentException("accountId is required");
        if (ledger == null || available == null || hold == null) throw new IllegalArgumentException("balances are required");
        if (ledger.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("ledger must be >= 0");
        if (available.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("available must be >= 0");
        if (hold.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("hold must be >= 0");

        jpa.initIfAbsent(accountId, ledger, available, hold);
        // si devuelve 0 => ya existía, lo cual es OK (idempotente)
    }


  @Override
  public void initZero(UUID accountId) {
    AccountBalanceEntity e = new AccountBalanceEntity();
    e.setAccount(em.getReference(AccountEntity.class, accountId));
    //e.setAccountId(accountId);
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
