package com.tagokoder.account.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface ReleaseHoldUseCase {
    record Command(UUID accountId, String currency, BigDecimal amount, String reasonOrNull) {}
    record Result(boolean ok, BigDecimal newHold) {}
    Result release(Command command);
}
