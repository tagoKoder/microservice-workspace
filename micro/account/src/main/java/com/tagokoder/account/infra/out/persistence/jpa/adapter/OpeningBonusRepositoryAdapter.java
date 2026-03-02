package com.tagokoder.account.infra.out.persistence.jpa.adapter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tagokoder.account.domain.port.out.OpeningBonusRepositoryPort;
import com.tagokoder.account.infra.out.persistence.jpa.SpringDataOpeningBonusGrantJpa;
import com.tagokoder.account.infra.out.persistence.jpa.entity.OpeningBonusGrantEntity;

@Component
public class OpeningBonusRepositoryAdapter implements OpeningBonusRepositoryPort {

  private final SpringDataOpeningBonusGrantJpa jpa;

  public OpeningBonusRepositoryAdapter(SpringDataOpeningBonusGrantJpa jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<BonusGrant> findByKey(String key) {
    if (key == null || key.isBlank()) return Optional.empty();

    return jpa.findByIdempotencyKey(key).map(e -> new BonusGrant(
        e.getIdempotencyKey(),
        mapStatus(e.getStatus()),
        e.getAccountId(),
        e.getJournalId(),
        e.getAmount(),
        e.getCurrency()
    ));
  }

  @Override
  @Transactional
  public boolean tryAcquire(String key, BigDecimal amount, String currency) {
    if (key == null || key.isBlank()) throw new IllegalArgumentException("key is required");
    if (amount == null) throw new IllegalArgumentException("amount is required");
    if (currency == null || currency.isBlank()) throw new IllegalArgumentException("currency is required");

    try {
      OpeningBonusGrantEntity e = new OpeningBonusGrantEntity();
      e.setIdempotencyKey(key);
      e.setStatus(OpeningBonusGrantEntity.GrantStatus.PENDING);
      e.setAmount(amount);
      e.setCurrency(currency);
      e.setLockedAt(OffsetDateTime.now(ZoneOffset.UTC));
      e.setLockedBy("svc"); // opcional (debug)

      jpa.save(e);
      return true; // ✅ yo inserté (gané)
    } catch (DataIntegrityViolationException dup) {
      return false; // ya existía
    }
  }

  @Override
  @Transactional
  public boolean attachAccountIfEmpty(String key, UUID accountId) {
    if (key == null || key.isBlank()) throw new IllegalArgumentException("key is required");
    if (accountId == null) throw new IllegalArgumentException("accountId is required");
    return jpa.attachAccountIfEmpty(key, accountId) == 1;
  }

  @Override
  @Transactional
  public boolean completeIfPending(String key, UUID accountId, String journalId) {
    if (key == null || key.isBlank()) throw new IllegalArgumentException("key is required");
    if (accountId == null) throw new IllegalArgumentException("accountId is required");
    if (journalId == null || journalId.isBlank()) throw new IllegalArgumentException("journalId is required");
    return jpa.completeIfPending(key, accountId, journalId) == 1;
  }

  private static Status mapStatus(OpeningBonusGrantEntity.GrantStatus s) {
    if (s == null) return Status.PENDING;
    return switch (s) {
      case PENDING -> Status.PENDING;
      case COMPLETED -> Status.COMPLETED;
    };
  }
}