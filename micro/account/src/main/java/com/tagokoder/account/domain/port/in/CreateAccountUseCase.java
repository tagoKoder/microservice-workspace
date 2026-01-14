package com.tagokoder.account.domain.port.in;

import java.util.UUID;

public interface CreateAccountUseCase {

    record Command(UUID customerId, String productType, String currency) {}
    record Result(UUID accountId) {}

    Result create(Command command);
}