package com.tagokoder.account.domain.port.in;

import java.util.UUID;

public interface GetAccountByNumberUseCase {

    record Command(String accountNumber, boolean includeInactive) {}

    record AccountLookupView(
            UUID accountId,
            UUID customerId,
            String accountNumber,   // "000000012345"
            String displayName,
            String currency,
            String status,
            String productType
    ) {}

    record Result(AccountLookupView account) {}

    Result getByNumber(Command c);
}