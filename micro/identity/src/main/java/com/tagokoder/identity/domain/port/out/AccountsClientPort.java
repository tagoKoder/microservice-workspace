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

  record OpenedAccount(
      String accountId,
      String accountNumber,
      String bonusJournalId,
      String status
  ) {}

  OpenedAccount openAccountWithOpeningBonus(
      String bearer,
      String customerId,
      String currency,
      String productType,     // "CHECKING" | "SAVINGS"
      String idempotencyKey,  // act:<regId>:checking
      String externalRef,     // act:<regId>:checking
      String initiatedBy      // "svc:identity"
  );
}
