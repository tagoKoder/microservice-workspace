package com.tagokoder.identity.application.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tagokoder.identity.application.IdentityClientsProperties;
import com.tagokoder.identity.domain.model.RegistrationIntent;
import com.tagokoder.identity.domain.model.kyc.FinalizedObject;
import com.tagokoder.identity.domain.port.in.ActivateRegistrationUseCase;
import com.tagokoder.identity.domain.port.in.ConfirmRegistrationKycUseCase;
import com.tagokoder.identity.domain.port.in.StartRegistrationUseCase;
import com.tagokoder.identity.domain.port.out.AccountsClientPort;
import com.tagokoder.identity.domain.port.out.IdentityLinkRepositoryPort;
import com.tagokoder.identity.domain.port.out.KycPresignedStoragePort;
import com.tagokoder.identity.domain.port.out.LedgerPaymentsClientPort;
import com.tagokoder.identity.domain.port.out.RegistrationIntentRepositoryPort;

@Service
public class OnboardingService implements StartRegistrationUseCase, ConfirmRegistrationKycUseCase, ActivateRegistrationUseCase {

  private final RegistrationIntentRepositoryPort registrationRepo;
  private final KycPresignedStoragePort kycStorage;

  private final AccountsClientPort accounts;
  private final LedgerPaymentsClientPort ledger;
  private final IdentityClientsProperties clientProps;
  private final IdentityLinkRepositoryPort identityLinks;

  public OnboardingService(
    RegistrationIntentRepositoryPort registrationRepo,
    KycPresignedStoragePort kycStorage,
    AccountsClientPort accounts,
    LedgerPaymentsClientPort ledger,
    IdentityClientsProperties clientProps,
    IdentityLinkRepositoryPort identityLinks
  ) {
    this.registrationRepo = registrationRepo;
    this.kycStorage = kycStorage;
    this.accounts = accounts;
    this.ledger = ledger;
    this.clientProps = clientProps;
    this.identityLinks = identityLinks;
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

    reg.setMonthlyIncome(c.monthlyIncome());
    reg.setOccupationType(c.occupationType());

    registrationRepo.save(reg);

    return new StartRegistrationResponse(regId, "started", now, uploads);
  }

  @Override
  @Transactional
  public ConfirmRegistrationKycResult confirm(ConfirmRegistrationKycCommand command) {
    UUID regId = command.registrationId();
    if (regId == null) throw new IllegalArgumentException("registrationId required");

    RegistrationIntent reg = registrationRepo.findById(regId)
      .orElseThrow(() -> new IllegalStateException("registration not found"));

    List<FinalizedObject> finalized = kycStorage.confirmAndFinalize(regId, command.objects());

    for (FinalizedObject f : finalized) {
        reg.getKycObjects().removeIf(x -> x.kind() == f.kind());
        reg.addKycObject(f);
    }

    reg.setState(RegistrationIntent.State.KYC_CONFIRMED);
    reg.setUpdatedAt(Instant.now());

    registrationRepo.save(reg);

    return new ConfirmRegistrationKycResult(regId, "kyc_confirmed", reg.getUpdatedAt(), finalized);
  }

  @Override
  @Transactional
  public ActivateRegistrationResult activate(ActivateRegistrationCommand c) {
    UUID regId = c.registrationId();

    RegistrationIntent reg = registrationRepo.findById(regId)
      .orElseThrow(() -> new IllegalStateException("registration not found"));

    if (reg.getState() == RegistrationIntent.State.ACTIVATED) {
      return buildResponse(reg, c.correlationId());
    }
    if (reg.getState() != RegistrationIntent.State.KYC_CONFIRMED &&
        reg.getState() != RegistrationIntent.State.ACTIVATING) {
      throw new IllegalStateException("registration must be KYC_CONFIRMED to activate");
    }
    if (!c.acceptedTerms()) throw new IllegalArgumentException("accepted_terms required");

    if (reg.getActivationRef() == null || reg.getActivationRef().isBlank()) {
      reg.setActivationRef("act:" + regId);
    }
    reg.setState(RegistrationIntent.State.ACTIVATING);
    reg.setUpdatedAt(Instant.now());
    registrationRepo.save(reg);

    final String bearer = clientProps.getServiceAccessToken(); // flujo público
    final String activationRef = reg.getActivationRef();

    // (1) CreateCustomer idempotente
    if (reg.getCustomerId() == null || reg.getCustomerId().isBlank()) {
      String customerId = accounts.createCustomer(
        bearer,
        activationRef + ":customer",
        regId.toString(),
        c.fullName(),
        c.birthDate(),
        c.tin(),
        c.email(),
        c.phone()      
      );
      reg.setCustomerId(customerId);
      reg.setUpdatedAt(Instant.now());
      registrationRepo.save(reg);
      identityLinks.upsert(reg.getIdentityId(), reg.getCustomerId());

    }

    // (2) Create CHECKING
    if (reg.getPrimaryAccountId() == null || reg.getPrimaryAccountId().isBlank()) {
      String accId = accounts.createAccount(
        bearer,
        activationRef + ":checking",
        regId + ":CHECKING",
        reg.getCustomerId(),
        "USD",
        "CHECKING"
      );
      reg.setPrimaryAccountId(accId);
      reg.setUpdatedAt(Instant.now());
      registrationRepo.save(reg);
    }

    // (3) Bonus opcional
    if (reg.getBonusJournalId() == null || reg.getBonusJournalId().isBlank()) {
      String journalId = ledger.creditAccount(
        bearer,
        activationRef + ":bonus",
        reg.getPrimaryAccountId(),
        "USD",
        "50.00",
        "registration_bonus",
        reg.getCustomerId()
      );
      reg.setBonusJournalId(journalId);
      reg.setUpdatedAt(Instant.now());
      registrationRepo.save(reg);
    }

    reg.setState(RegistrationIntent.State.ACTIVATED);
    reg.setActivatedAt(Instant.now());
    reg.setUpdatedAt(Instant.now());
    registrationRepo.save(reg);

    return buildResponse(reg, c.correlationId());
  }

  private ActivateRegistrationResult buildResponse(RegistrationIntent reg, String correlationId) {
    List<ActivatedAccount> accounts = new ArrayList<>();
    if (reg.getPrimaryAccountId() != null && !reg.getPrimaryAccountId().isBlank()) {
      accounts.add(new ActivatedAccount(reg.getPrimaryAccountId(), "USD", "CHECKING"));
    }
    if (reg.getSavingsAccountId() != null && !reg.getSavingsAccountId().isBlank()) {
      accounts.add(new ActivatedAccount(reg.getSavingsAccountId(), "USD", "SAVINGS"));
    }

    return new ActivateRegistrationResult(
      reg.getId(),
      reg.getState().name(),
      reg.getCustomerId(),
      reg.getPrimaryAccountId(),
      reg.getActivationRef(),
      accounts,
      reg.getBonusJournalId(),
      correlationId == null ? "" : correlationId
    );
  }
}
