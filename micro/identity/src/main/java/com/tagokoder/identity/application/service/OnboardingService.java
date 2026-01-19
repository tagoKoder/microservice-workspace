package com.tagokoder.identity.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tagokoder.identity.domain.model.RegistrationIntent;
import com.tagokoder.identity.domain.model.kyc.FinalizedObject;
import com.tagokoder.identity.domain.model.kyc.KycDocumentKind;
import com.tagokoder.identity.domain.port.in.ConfirmRegistrationKycUseCase;
import com.tagokoder.identity.domain.port.in.StartRegistrationUseCase;
import com.tagokoder.identity.domain.port.out.KycPresignedStoragePort;
import com.tagokoder.identity.domain.port.out.RegistrationIntentRepositoryPort;

@Service
public class OnboardingService implements StartRegistrationUseCase, ConfirmRegistrationKycUseCase {

    private final RegistrationIntentRepositoryPort registrationRepo;
    private final KycPresignedStoragePort kycStorage;

    public OnboardingService(RegistrationIntentRepositoryPort registrationRepo,
                             KycPresignedStoragePort kycStorage) {
        this.registrationRepo = registrationRepo;
        this.kycStorage = kycStorage;
    }

    @Override
    @Transactional
    public StartRegistrationResponse start(StartRegistrationCommand c) {
        UUID regId = UUID.randomUUID();
        Instant now = Instant.now();

        var uploads = kycStorage.issuePresignedUploads(regId, c.idFrontContentType(), c.selfieContentType());

        RegistrationIntent reg = new RegistrationIntent();
        reg.setId(regId);
        reg.setState(RegistrationIntent.State.STARTED);
        reg.setCreatedAt(now);
        reg.setUpdatedAt(now);

        reg.setChannel(c.channel());
        reg.setEmail(c.email());
        reg.setPhone(c.phone());

        reg.setNationalId(c.nationalId());
        reg.setNationalIdIssueDate(c.nationalIdIssueDate());
        reg.setFingerprintCode(c.fingerprintCode());

        registrationRepo.save(reg);

        return new StartRegistrationResponse(regId, "started", now, uploads);
    }

    @Override
    public ConfirmRegistrationKycResult confirm(ConfirmRegistrationKycCommand command) {
        UUID regId = command.registrationId();
        if (regId == null) throw new IllegalArgumentException("registrationId required");

        RegistrationIntent reg = registrationRepo.findById(regId)
                .orElseThrow(() -> new IllegalStateException("registration not found"));

        List<FinalizedObject> finalized = kycStorage.confirmAndFinalize(regId, command.objects());

        FinalizedObject idFront = finalized.stream()
                .filter(x -> x.kind() == KycDocumentKind.ID_FRONT).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("missing finalized ID_FRONT"));

        FinalizedObject selfie = finalized.stream()
                .filter(x -> x.kind() == KycDocumentKind.SELFIE).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("missing finalized SELFIE"));

        reg.setIdFrontBucket(idFront.bucket());
        reg.setIdFrontKey(idFront.key());
        reg.setSelfieBucket(selfie.bucket());
        reg.setSelfieKey(selfie.key());

        reg.setState(RegistrationIntent.State.KYC_CONFIRMED);
        reg.setUpdatedAt(Instant.now());

        registrationRepo.save(reg);

        return new ConfirmRegistrationKycResult(regId, "kyc_confirmed", reg.getUpdatedAt(), finalized);
        
    }
}
