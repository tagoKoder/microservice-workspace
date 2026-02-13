package com.tagokoder.account.domain.port.in;

import java.util.List;
import java.util.UUID;

public interface BatchGetAccountSummariesUseCase {

    record Command(List<UUID> accountIds, boolean includeInactive) {}

    record AccountSummaryView(
            UUID accountId,
            String accountNumber,
            String productType, // "checking" | "savings"
            String currency,
            String status,
            String displayName
    ) {}

    record Missing(UUID accountId, String reason) {}

    record Result(List<AccountSummaryView> accounts, List<Missing> missing) {}

    Result batchGetSummaries(Command c);
}