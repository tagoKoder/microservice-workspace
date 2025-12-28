package com.tagokoder.account.domain.port.out;

import java.util.Optional;
import java.util.UUID;

public interface AccountBalanceRepositoryPort {
    Optional<BalancesRow> findByAccountId(UUID accountId);
    void initZero(UUID accountId);

    Double incrementHold(UUID accountId, Double amount);
    Double decrementHold(UUID accountId, Double amount);
    record BalancesRow(double ledger, double available, double hold) {}
}
