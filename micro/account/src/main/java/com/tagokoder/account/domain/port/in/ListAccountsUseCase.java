package com.tagokoder.account.domain.port.in;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ListAccountsUseCase {

    record AccountView(
            UUID id,
            UUID customerId,
            String productType,
            String currency,
            String status,
            java.time.OffsetDateTime openedAt,
            java.time.OffsetDateTime updatedAt,
            Balances balances
    ) {}

    record Balances(BigDecimal ledger, BigDecimal available, BigDecimal hold) {}

    record Result(List<AccountView> accounts) {}

    Result listByCustomer(UUID customerId);
}
