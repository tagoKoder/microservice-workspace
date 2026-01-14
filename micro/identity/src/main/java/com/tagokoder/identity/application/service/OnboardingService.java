package com.tagokoder.identity.application.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tagokoder.identity.domain.model.RegistrationIntent;
import com.tagokoder.identity.domain.port.in.StartRegistrationUseCase;
import com.tagokoder.identity.domain.port.out.KycDocumentStoragePort;
import com.tagokoder.identity.domain.port.out.RegistrationIntentRepositoryPort;

@Service
public class OnboardingService implements StartRegistrationUseCase {

    private final RegistrationIntentRepositoryPort registrationRepo;
    private final KycDocumentStoragePort storage;

    public OnboardingService(RegistrationIntentRepositoryPort registrationRepo,
                             KycDocumentStoragePort storage) {
        this.registrationRepo = registrationRepo;
        this.storage = storage;
    }

    @Override
    @Transactional
    public StartRegistrationResponse start(StartRegistrationCommand c) {
        UUID id = UUID.randomUUID();

        List<KycDocumentStoragePort.StoredObject> created = new ArrayList<>();
        try {
            // content-type: idealmente viene del request; como tu proto no lo trae, usa octet-stream o sniff
            String idCt = "application/octet-stream";
            String selfieCt = "application/octet-stream";

            var idObj = storage.store(id.toString(), "ID_FRONT", c.idDocumentFront(), idCt);
            created.add(idObj);

            var selfieObj = storage.store(id.toString(), "SELFIE", c.selfie(), selfieCt);
            created.add(selfieObj);

            Instant now = Instant.now();

            RegistrationIntent intent = new RegistrationIntent(
                id,
                c.email(),
                c.phone(),
                c.channel(),
                RegistrationIntent.State.STARTED,
                c.nationalId(),
                c.nationalIdIssueDate(),
                c.fingerprintCode(),

                // IMPORTANTE: ya no guardes URLs públicas
                // guarda referencias (bucket/key) en campos del dominio (ver punto 5)
                idObj.bucket(), idObj.key(),
                selfieObj.bucket(), selfieObj.key(),

                BigDecimal.valueOf(c.monthlyIncome()),
                c.occupationType(),
                now,
                now
            );

            RegistrationIntent saved = registrationRepo.save(intent);

            return new StartRegistrationResponse(
                saved.getId(),
                saved.getState().name().toLowerCase(),
                saved.getCreatedAt()
            );

        } catch (Exception e) {
            // rollback best-effort
            for (int i = created.size() - 1; i >= 0; i--) {
                storage.delete(created.get(i));
            }
            throw e;
        }
    }
}
