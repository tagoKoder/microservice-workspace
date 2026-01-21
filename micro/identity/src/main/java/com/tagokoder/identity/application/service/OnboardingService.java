package com.tagokoder.identity.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tagokoder.identity.domain.model.RegistrationIntent;
import com.tagokoder.identity.domain.model.kyc.FinalizedObject;
import com.tagokoder.identity.domain.port.in.ActivateRegistrationUseCase;
import com.tagokoder.identity.domain.port.in.ConfirmRegistrationKycUseCase;
import com.tagokoder.identity.domain.port.in.StartRegistrationUseCase;
import com.tagokoder.identity.domain.port.out.KycPresignedStoragePort;
import com.tagokoder.identity.domain.port.out.RegistrationIntentRepositoryPort;

@Service
public class OnboardingService implements StartRegistrationUseCase, ConfirmRegistrationKycUseCase, ActivateRegistrationUseCase  {

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

        // Upsert FINAL objects en la lista (dominio)
        for (FinalizedObject f : finalized) {
            reg.addKycObject(f);
        }

        reg.setState(RegistrationIntent.State.KYC_CONFIRMED);
        reg.setUpdatedAt(Instant.now());

        registrationRepo.save(reg);

        return new ConfirmRegistrationKycResult(regId, "kyc_confirmed", reg.getUpdatedAt(), finalized);
        
    }

  @Override
  public ActivateRegistrationResult activate(ActivateRegistrationCommand c) {
    UUID regId = c.registrationId();

    // (1) Tx corta: validar y preparar activationRef
    RegistrationIntent reg = registrationRepo.findById(regId)
      .orElseThrow(() -> new IllegalStateException("registration not found"));

    if (reg.getState() == RegistrationIntent.State.ACTIVATED) {
      return buildResponse(reg);
    }
    if (reg.getState() != RegistrationIntent.State.KYC_CONFIRMED &&
        reg.getState() != RegistrationIntent.State.ACTIVATING) {
      throw new IllegalStateException("registration must be KYC_CONFIRMED to activate");
    }
    if (!c.acceptedTerms()) {
      throw new IllegalArgumentException("accepted_terms required");
    }

    if (reg.getActivationRef() == null || reg.getActivationRef().isBlank()) {
      reg.setActivationRef("act:" + regId); // estable
    }
    reg.setState(RegistrationIntent.State.ACTIVATING);
    registrationRepo.save(reg);

    String activationRef = reg.getActivationRef();

    // (2) Paso: CreateCustomer (idempotente en Accounts)
    if (reg.getCustomerId() == null) {
      String customerId = accounts.createCustomer(
        activationRef,
        regId.toString(),
        c.fullName(), c.birthDate(), c.tin(),
        c.email(), c.phone(), c.country()
      );
      reg = registrationRepo.findById(regId).orElseThrow();
      reg.setCustomerId(customerId);
      registrationRepo.save(reg);
    }

    // (3) Paso: Create CHECKING
    if (reg.getPrimaryAccountId() == null) {
      String accId = accounts.createAccount(
        activationRef + ":checking",
        regId + ":CHECKING",
        reg.getCustomerId(),
        "USD",
        "CHECKING"
      );
      reg = registrationRepo.findById(regId).orElseThrow();
      reg.setPrimaryAccountId(accId);
      registrationRepo.save(reg);
    }

    // (4) (Opcional) bonus en ledger (ya tiene idempotency_key)
    if (reg.getBonusJournalId() == null) {
      String journalId = ledger.creditAccount(
        activationRef + ":bonus",
        reg.getPrimaryAccountId(),
        "USD",
        "5.00",
        "registration_bonus",
        reg.getCustomerId()
      );
      reg = registrationRepo.findById(regId).orElseThrow();
      reg.setBonusJournalId(journalId);
      registrationRepo.save(reg);
    }

    // (5) Finalizar
    reg = registrationRepo.findById(regId).orElseThrow();
    reg.setState(RegistrationIntent.State.ACTIVATED);
    reg.setActivatedAt(Instant.now());
    registrationRepo.save(reg);

    return buildResponse(reg);
  }
}
