package com.tagokoder.account.domain.port.out;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface AccountLimitsRepositoryPort {
    Optional<LimitsRow> findByAccountId(UUID accountId);
    LimitsRow patch(UUID accountId, BigDecimal dailyOut, BigDecimal dailyIn);
    

    record LimitsRow(BigDecimal dailyOut, BigDecimal dailyIn) {}
}
