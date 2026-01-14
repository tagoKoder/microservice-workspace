package com.tagokoder.account.domain.port.in;

import java.util.UUID;

public interface PatchCustomerUseCase {

    record Command(
            UUID customerId,
            String fullNameOrNull,
            String riskSegmentOrNull,
            String customerStatusOrNull,
            ContactPatch contactOrNull,
            PreferencesPatch preferencesOrNull
    ) {}

    record ContactPatch(String emailOrNull, String phoneOrNull) {}
    record PreferencesPatch(String channelOrNull, Boolean optInOrNull) {}

    record Result(UUID customerId) {}

    Result patch(Command command);
}
