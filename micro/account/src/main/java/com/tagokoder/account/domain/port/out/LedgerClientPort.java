package com.tagokoder.account.domain.port.out;

import java.util.UUID;

public interface LedgerClientPort {
    String creditAccount(
            String idempotencyKey,
            UUID accountId,
            String currency,
            String amount,       // "50.00"
            String initiatedBy,  // "system" o subject
            String externalRef,  // "bonus:registration"
            String reason,       // "registration_bonus"
            UUID customerId
    );
}