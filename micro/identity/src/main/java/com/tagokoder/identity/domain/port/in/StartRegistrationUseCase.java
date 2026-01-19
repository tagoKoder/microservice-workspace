package com.tagokoder.identity.domain.port.in;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.tagokoder.identity.domain.model.kyc.PresignedUpload;

public interface StartRegistrationUseCase {

    StartRegistrationResponse start(StartRegistrationCommand command);

    public record StartRegistrationCommand(
        String channel,
        String nationalId,
        LocalDate nationalIdIssueDate,
        String fingerprintCode,
        double monthlyIncome,
        String occupationType,
        String email,
        String phone,
        String idFrontContentType,
        String selfieContentType
) {}


    record StartRegistrationResponse(
        UUID registrationId,
        String state,
        Instant createdAt,
        List<PresignedUpload> uploads
    ) {}
}

