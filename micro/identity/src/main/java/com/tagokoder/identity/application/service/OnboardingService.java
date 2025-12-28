package com.tagokoder.identity.application.service;

import com.tagokoder.identity.domain.model.RegistrationIntent;
import com.tagokoder.identity.domain.port.in.StartRegistrationUseCase;
import com.tagokoder.identity.domain.port.out.KycDocumentStoragePort;
import com.tagokoder.identity.domain.port.out.RegistrationIntentRepositoryPort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

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
    public StartRegistrationResponse start(StartRegistrationCommand c) {
        UUID id = UUID.randomUUID();

        // 1) Guardar imágenes vía puerto de storage
        byte[] idBytes = Base64.getDecoder().decode(c.idDocumentFrontBase64());
        String idUrl = storage.store(
                id.toString(),
                "ID_FRONT",
                new ByteArrayInputStream(idBytes)
        );

        byte[] selfieBytes = Base64.getDecoder().decode(c.selfieBase64());
        String selfieUrl = storage.store(
                id.toString(),
                "SELFIE",
                new ByteArrayInputStream(selfieBytes)
        );

        Instant now = Instant.now();

        // 2) Construir el agregado de dominio
        RegistrationIntent intent = new RegistrationIntent(
                id,
                c.email(),
                c.phone(),
                c.channel(),
                RegistrationIntent.State.STARTED,
                c.nationalId(),
                c.nationalIdIssueDate(),
                c.fingerprintCode(),
                idUrl,
                selfieUrl,
                BigDecimal.valueOf(c.monthlyIncome()),
                c.occupationType(),
                now,
                now
        );

        // 3) Persistir usando el port
        RegistrationIntent saved = registrationRepo.save(intent);

        // 4) Respuesta DTO para el caso de uso
        return new StartRegistrationResponse(
                saved.getId(),
                saved.getState().name().toLowerCase(), // "started"
                saved.getCreatedAt()
        );
    }
}