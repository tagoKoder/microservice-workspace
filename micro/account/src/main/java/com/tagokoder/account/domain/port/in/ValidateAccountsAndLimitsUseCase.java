package com.tagokoder.account.domain.port.in;

import java.util.UUID;

public interface ValidateAccountsAndLimitsUseCase {

    record Command(UUID sourceAccountId,
                   UUID destinationAccountId,
                   String currency,
                   Double amount) {}

    record Result(boolean ok, String reasonOrNull) {}

    Result validate(Command command);
}
