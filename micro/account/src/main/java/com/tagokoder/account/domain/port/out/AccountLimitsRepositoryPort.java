package com.tagokoder.account.domain.port.out;

import java.util.Optional;
import java.util.UUID;

public interface AccountLimitsRepositoryPort {
    Optional<LimitsRow> findByAccountId(UUID accountId);
    LimitsRow patch(UUID accountId, Double dailyOut, Double dailyIn);

    record LimitsRow(double dailyOut, double dailyIn) {}
}
