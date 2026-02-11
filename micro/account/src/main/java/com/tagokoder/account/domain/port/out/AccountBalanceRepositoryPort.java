package com.tagokoder.account.domain.port.out;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface AccountBalanceRepositoryPort {
    Optional<BalancesRow> findByAccountId(UUID accountId);
    void init(UUID accountId, BigDecimal ledger, BigDecimal available, BigDecimal hold);
    void initZero(UUID accountId);

    BigDecimal incrementHold(UUID accountId, BigDecimal amount);
    BigDecimal decrementHold(UUID accountId, BigDecimal amount);
    record BalancesRow(BigDecimal ledger, BigDecimal available, BigDecimal hold) {}
}
