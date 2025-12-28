package com.tagokoder.identity.domain.port.in;
import java.time.Instant;
import java.util.UUID;

public interface StartRegistrationUseCase {

    StartRegistrationResponse start(StartRegistrationCommand command);

    public record StartRegistrationCommand(
        String channel,
        String nationalId,
        java.time.LocalDate nationalIdIssueDate,
        String fingerprintCode,
        String idDocumentFrontBase64,
        String selfieBase64,
        double monthlyIncome,
        String occupationType,
        String email,
        String phone
) {}


    record StartRegistrationResponse(
            UUID registrationId,
            String state,
            Instant createdAt
    ) {}
}

