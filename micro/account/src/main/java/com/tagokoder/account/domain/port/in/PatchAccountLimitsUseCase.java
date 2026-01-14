package com.tagokoder.account.domain.port.in;

import java.util.UUID;

public interface PatchAccountLimitsUseCase {

    record Command(UUID accountId, Double dailyOut, Double dailyIn) {}
    record Result(UUID accountId, double dailyOut, double dailyIn) {}

    Result patch(Command command);
}
