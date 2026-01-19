package com.tagokoder.identity.domain.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.tagokoder.identity.domain.model.kyc.FinalizedObject;
import com.tagokoder.identity.domain.model.kyc.UploadedObject;

public interface ConfirmRegistrationKycUseCase {

    ConfirmRegistrationKycResult confirm(ConfirmRegistrationKycCommand command);

    public record ConfirmRegistrationKycCommand(
        UUID registrationId,
        List<UploadedObject> objects
    ) {}
    
    public record ConfirmRegistrationKycResult(
            UUID registrationId,
            String state,
            Instant updatedAt,
            List<FinalizedObject> finalized
    ) {}
}
