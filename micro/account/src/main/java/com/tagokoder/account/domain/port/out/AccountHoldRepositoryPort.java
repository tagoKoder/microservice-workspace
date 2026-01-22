package com.tagokoder.account.domain.port.out;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface AccountHoldRepositoryPort {
  Optional<HoldRow> findByIdempotencyKey(String idempotencyKey);
  Optional<HoldRow> find(UUID accountId, UUID holdId);
  void insertReserved(UUID accountId, UUID holdId, String currency, BigDecimal amount, String idempotencyKey);
  void markReleased(UUID accountId, UUID holdId);

  record HoldRow(UUID accountId, UUID holdId, String currency, BigDecimal amount, String status, String idempotencyKey) {}
}
