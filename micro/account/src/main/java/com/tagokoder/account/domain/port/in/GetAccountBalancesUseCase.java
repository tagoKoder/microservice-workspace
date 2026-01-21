package com.tagokoder.account.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface GetAccountBalancesUseCase {

    record Result(UUID accountId, BigDecimal ledger, BigDecimal available, BigDecimal hold) {}

    Result get(UUID accountId);
}
