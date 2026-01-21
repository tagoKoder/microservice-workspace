package com.tagokoder.identity.domain.port.out;

public interface LedgerPaymentsClientPort {
  String creditAccount(
    String bearerToken,
    String idempotencyKey,
    String accountId,
    String currency,
    String amount,
    String reason,
    String customerId
  );
}