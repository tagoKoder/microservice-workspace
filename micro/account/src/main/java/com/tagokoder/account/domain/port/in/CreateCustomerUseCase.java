package com.tagokoder.account.domain.port.in;

import java.util.UUID;

public interface CreateCustomerUseCase {

    record Command(
            String fullName,
            java.time.LocalDate birthDate,
            String tin,
            String riskSegment,
            String email,
            String phone,
            Address addressOrNull
    ) {}

    record Address(
            String country,
            String line1,
            String line2,
            String city,
            String province,
            String postalCode
    ) {}

    record Result(UUID customerId) {}

    Result create(Command command);
}
