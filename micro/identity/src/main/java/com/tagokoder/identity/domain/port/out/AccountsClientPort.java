package com.tagokoder.identity.domain.port.out;

import java.time.LocalDate;

public interface AccountsClientPort {
  String createCustomer(
    String bearerToken,
    String idempotencyKey,
    String externalRef,
    String fullName,
    LocalDate birthDate,
    String tin,
    String email,
    String phone
  );

  String createAccount(
    String bearerToken,
    String idempotencyKey,
    String externalRef,
    String customerId,
    String currency,
    String productType
  );
}
