package com.tagokoder.account.domain.port.in;

import java.util.UUID;

public interface OpenAccountWithOpeningBonusUseCase {

    record Command(
            UUID customerId,
            String productType,
            String currency,
            String idempotencyKey, // recomendado
            String initiatedBy     // "system" o subject
    ) {}

    record Result(
            UUID accountId,
            String bonusJournalId, // journal_id de ledger
            String status          // "opened"
    ) {}

    Result open(Command command);
}