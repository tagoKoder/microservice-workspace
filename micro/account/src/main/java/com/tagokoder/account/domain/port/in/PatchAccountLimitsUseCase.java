package com.tagokoder.account.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface PatchAccountLimitsUseCase {

    record Command(UUID accountId, BigDecimal dailyOut, BigDecimal dailyIn) {}
    record Result(UUID accountId, BigDecimal dailyOut, BigDecimal dailyIn) {}

    Result patch(Command command);
}
