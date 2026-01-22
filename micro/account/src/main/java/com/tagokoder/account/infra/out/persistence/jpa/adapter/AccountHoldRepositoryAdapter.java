package com.tagokoder.account.infra.out.persistence.jpa.adapter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tagokoder.account.domain.port.out.AccountHoldRepositoryPort;
import com.tagokoder.account.infra.out.persistence.jpa.SpringDataAccountHoldJpa;
import com.tagokoder.account.infra.out.persistence.jpa.entity.AccountHoldEntity;
import com.tagokoder.account.infra.out.persistence.jpa.entity.AccountHoldId;

@Component
public class AccountHoldRepositoryAdapter implements AccountHoldRepositoryPort {

  private final SpringDataAccountHoldJpa jpa;

  public AccountHoldRepositoryAdapter(SpringDataAccountHoldJpa jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<HoldRow> findByIdempotencyKey(String idempotencyKey) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) return Optional.empty();
    return jpa.findByIdempotencyKey(idempotencyKey).map(this::toRow);
  }

  @Override
  public Optional<HoldRow> find(UUID accountId, UUID holdId) {
    AccountHoldId id = new AccountHoldId();
    id.setAccountId(accountId);
    id.setHoldId(holdId);
    return jpa.findById(id).map(this::toRow);
  }

  @Override
  @Transactional
  public void insertReserved(UUID accountId, UUID holdId, String currency, BigDecimal amount, String idempotencyKey) {
    AccountHoldId id = new AccountHoldId();
    id.setAccountId(accountId);
    id.setHoldId(holdId);

    AccountHoldEntity e = new AccountHoldEntity();
    e.setId(id);
    e.setCurrency(currency);
    e.setAmount(amount);
    e.setStatus("reserved");
    e.setIdempotencyKey(idempotencyKey);
    e.setCreatedAt(OffsetDateTime.now());
    e.setUpdatedAt(OffsetDateTime.now());
    jpa.save(e);
  }

  @Override
  @Transactional
  public void markReleased(UUID accountId, UUID holdId) {
    AccountHoldId id = new AccountHoldId();
    id.setAccountId(accountId);
    id.setHoldId(holdId);

    var e = jpa.findById(id).orElseThrow(() -> new IllegalArgumentException("hold not found"));
    e.setStatus("released");
    e.setUpdatedAt(OffsetDateTime.now());
    jpa.save(e);
  }

  private HoldRow toRow(AccountHoldEntity e) {
    return new HoldRow(
      e.getId().getAccountId(),
      e.getId().getHoldId(),
      e.getCurrency(),
      e.getAmount(),
      e.getStatus(),
      e.getIdempotencyKey()
    );
  }
}
