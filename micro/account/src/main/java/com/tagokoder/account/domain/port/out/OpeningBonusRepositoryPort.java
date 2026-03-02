package com.tagokoder.account.domain.port.out;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface OpeningBonusRepositoryPort {

  enum Status { PENDING, COMPLETED }

  record BonusGrant(
      String idempotencyKey,
      Status status,
      UUID accountId,      // nullable mientras PENDING
      String journalId,    // nullable mientras PENDING
      BigDecimal amount,
      String currency
  ) {}

  Optional<BonusGrant> findByKey(String idempotencyKey);

  /** Inserta fila PENDING si no existe. True si el insert fue el que “ganó”. */
  boolean tryAcquire(String key, BigDecimal amount, String currency);

  /** Setea account_id solo si aún está null (para reintentos seguros). Retorna true si actualizó. */
  boolean attachAccountIfEmpty(String key, UUID accountId);

  /**
   * Completa PENDING→COMPLETED. Retorna true si esta llamada hizo la transición.
   * (Si ya estaba COMPLETED, retorna false)
   */
  boolean completeIfPending(String key, UUID accountId, String journalId);
}