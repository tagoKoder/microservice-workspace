package com.tagokoder.identity.domain.port.in;

import java.util.UUID;

import lombok.Builder;

public interface ActivateRegistrationUseCase {
  record ActivateRegistrationCommand(
      UUID registrationId,
      String channel,
      String fullName,
      String tin,
      java.time.LocalDate birthDate,
      String country,
      String email,
      String phone,
      boolean acceptedTerms,
      String correlationId
  ) {}

  record ActivateRegistrationResult(
      UUID registrationId,
      String state,
      String customerId,
      String primaryAccountId,
      String activationRef,
      java.util.List<ActivatedAccount> accounts,
      String bonusJournalId,
      String correlationId
  ) {}
  record ActivatedAccount(String accountId, String currency, String productType) {}

  ActivateRegistrationResult activate(ActivateRegistrationCommand cmd);
}
