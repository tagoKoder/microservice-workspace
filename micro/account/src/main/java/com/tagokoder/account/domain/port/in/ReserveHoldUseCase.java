package com.tagokoder.account.domain.port.in;

import java.util.UUID;

public interface ReserveHoldUseCase {
    record Command(UUID accountId, String currency, Double amount, String reasonOrNull) {}
    record Result(boolean ok, Double newHold) {}
    Result reserve(Command command);
}
