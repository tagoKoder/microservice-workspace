package com.tagokoder.account.domain.port.in;

import java.util.UUID;

public interface GetAccountBalancesUseCase {

    record Result(UUID accountId, double ledger, double available, double hold) {}

    Result get(UUID accountId);
}
