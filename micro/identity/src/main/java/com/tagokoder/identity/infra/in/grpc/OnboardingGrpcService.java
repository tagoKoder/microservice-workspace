package com.tagokoder.identity.infra.in.grpc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.google.protobuf.Timestamp;
import com.tagokoder.identity.domain.model.kyc.KycDocumentKind;
import com.tagokoder.identity.domain.model.kyc.UploadedObject;
import com.tagokoder.identity.domain.port.in.ActivateRegistrationUseCase;
import com.tagokoder.identity.domain.port.in.ActivateRegistrationUseCase.ActivateRegistrationCommand;
import com.tagokoder.identity.domain.port.in.ConfirmRegistrationKycUseCase;
import com.tagokoder.identity.domain.port.in.ConfirmRegistrationKycUseCase.ConfirmRegistrationKycCommand;
import com.tagokoder.identity.domain.port.in.StartRegistrationUseCase;
import com.tagokoder.identity.domain.port.in.StartRegistrationUseCase.StartRegistrationCommand;
import com.tagokoder.identity.infra.security.grpc.CorrelationServerInterceptor;

import bank.identity.v1.ActivateRegistrationRequest;
import bank.identity.v1.ActivateRegistrationResponse;
import bank.identity.v1.ActivatedAccount;
import bank.identity.v1.ConfirmRegistrationKycRequest;
import bank.identity.v1.ConfirmRegistrationKycResponse;
import bank.identity.v1.OnboardingServiceGrpc;
import bank.identity.v1.PresignedUpload;
import bank.identity.v1.RegistrationState;
import bank.identity.v1.StartRegistrationRequest;
import bank.identity.v1.StartRegistrationResponse;
import io.grpc.stub.StreamObserver;


@Component
public class OnboardingGrpcService extends OnboardingServiceGrpc.OnboardingServiceImplBase {

  private final StartRegistrationUseCase startRegistration;
  private final ConfirmRegistrationKycUseCase confirmKyc;
  private final ActivateRegistrationUseCase activateRegistrationUseCase;

  public OnboardingGrpcService(StartRegistrationUseCase startRegistration, ConfirmRegistrationKycUseCase confirmKyc, ActivateRegistrationUseCase activateRegistrationUseCase) {
    this.startRegistration = startRegistration;
    this.confirmKyc = confirmKyc;
    this.activateRegistrationUseCase = activateRegistrationUseCase;
  }

  @Override
  public void startRegistration(StartRegistrationRequest request,
                                StreamObserver<StartRegistrationResponse> responseObserver) {

        var res = startRegistration.start(new StartRegistrationCommand(
                request.getChannel(),
                request.getNationalId(),
                LocalDate.parse(request.getNationalIdIssueDate()),
                request.getFingerprintCode(),
                BigDecimal.valueOf(request.getMonthlyIncome()),
                request.getOccupationType().name(),
                request.getEmail(),
                request.getPhone(),
                request.getIdFrontContentType(),
                request.getSelfieContentType()
        ));

        StartRegistrationResponse.Builder b = StartRegistrationResponse.newBuilder()
                .setRegistrationId(res.registrationId().toString())
                .setState(toProtoState(res.state()))
                .setCreatedAt(toTs(res.createdAt()));

        for (var u : res.uploads()) {
            b.addUploads(PresignedUpload.newBuilder()
                    .setDocType(toProtoKind(u.kind()))
                    .setBucket(u.bucket())
                    .setKey(u.key())
                    .setUploadUrl(u.putUrl())
                    .setExpiresInSeconds(toTs(u.expiresAt()).getSeconds())
                    .setMaxBytes(u.maxBytes())
                    .setContentType(u.requiredContentType() == null ? "" : u.requiredContentType())
                    .build());
        }

        responseObserver.onNext(b.build());
        responseObserver.onCompleted();
  }

      @Override
    public void confirmRegistrationKyc(ConfirmRegistrationKycRequest request,
                                       StreamObserver<ConfirmRegistrationKycResponse> responseObserver) {

        UUID regId = UUID.fromString(request.getRegistrationId());

        List<UploadedObject> objs = request.getObjectsList().stream()
                .map(this::mapUploaded)
                .toList();

        var res = confirmKyc.confirm(new ConfirmRegistrationKycCommand(regId, objs));


        ConfirmRegistrationKycResponse resp = ConfirmRegistrationKycResponse.newBuilder()
                .setRegistrationId(res.registrationId().toString())
                .setState(toProtoState(res.state()))
                .setConfirmedAt(toTs(res.updatedAt()))
                .build();

        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }


      private RegistrationState toProtoState(Object domainState) {
      if (domainState == null) return RegistrationState.REGISTRATION_STATE_UNSPECIFIED;

      // si ya es el enum del proto
      if (domainState instanceof RegistrationState rs) return rs;

      // si es enum de dominio, o String
      String s = String.valueOf(domainState).toUpperCase();

      return switch (s) {
          case "STARTED", "REGISTRATION_STATE_STARTED" -> RegistrationState.REGISTRATION_STATE_STARTED;
          case "CONTACT_VERIFIED", "REGISTRATION_STATE_CONTACT_VERIFIED" -> RegistrationState.REGISTRATION_STATE_CONTACT_VERIFIED;
          case "CONSENTED", "REGISTRATION_STATE_CONSENTED" -> RegistrationState.REGISTRATION_STATE_CONSENTED;
          case "ACTIVATED", "REGISTRATION_STATE_ACTIVATED" -> RegistrationState.REGISTRATION_STATE_ACTIVATED;
          case "REJECTED", "REGISTRATION_STATE_REJECTED" -> RegistrationState.REGISTRATION_STATE_REJECTED;
          default -> RegistrationState.REGISTRATION_STATE_UNSPECIFIED;
      };
  }

    @Override
    public void activateRegistration(ActivateRegistrationRequest request,
        StreamObserver<ActivateRegistrationResponse> responseObserver) {
        String corrId = CorrelationServerInterceptor.getCorrelationId();

    var res = activateRegistrationUseCase.activate(
        new ActivateRegistrationCommand(
        UUID.fromString(request.getRegistrationId()),
        request.getChannel(),
        request.getFullName(),
        request.getTin(),
        LocalDate.parse(request.getBirthDate()),
        request.getCountry(),
        request.getEmail(),
        request.getPhone(),
        request.getAcceptedTerms(),
        corrId
        )
    );

    var b = ActivateRegistrationResponse.newBuilder()
        .setRegistrationId(res.registrationId().toString())
        .setState(RegistrationState.REGISTRATION_STATE_ACTIVATED)
        .setCustomerId(res.customerId() == null ? "" : res.customerId())
        .setPrimaryAccountId(res.primaryAccountId() == null ? "" : res.primaryAccountId())
        .setActivationRef(res.activationRef() == null ? "" : res.activationRef())
        .setBonusJournalId(res.bonusJournalId() == null ? "" : res.bonusJournalId())
        .setCorrelationId(res.correlationId() == null ? "" : res.correlationId());

    for (var a : res.accounts()) {
        b.addAccounts(ActivatedAccount.newBuilder()
        .setAccountId(a.accountId())
        .setCurrency(a.currency())
        .setProductType(a.productType())
        .build());
    }

    responseObserver.onNext(b.build());
    responseObserver.onCompleted();
    }

    
    private UploadedObject mapUploaded(bank.identity.v1.UploadedObject o) {
        return new UploadedObject(
                fromProtoKind(o.getDocType()),
                o.getBucket(),
                o.getKey(),
                o.getEtag(),
                (o.getSizeBytes() > 0) ? o.getSizeBytes() : null,
                o.getContentType()
        );
    }

      private bank.identity.v1.KycDocType toProtoKind(KycDocumentKind k) {
        return switch (k) {
            case ID_FRONT -> bank.identity.v1.KycDocType.KYC_DOC_TYPE_ID_FRONT;
            case SELFIE -> bank.identity.v1.KycDocType.KYC_DOC_TYPE_SELFIE;
        };
    }
        private KycDocumentKind fromProtoKind(bank.identity.v1.KycDocType k) {
        return switch (k) {
            case KYC_DOC_TYPE_ID_FRONT -> KycDocumentKind.ID_FRONT;
            case KYC_DOC_TYPE_SELFIE -> KycDocumentKind.SELFIE;
            default -> KycDocumentKind.ID_FRONT;
        };
    }

    private Timestamp toTs(java.time.Instant i) {
        if (i == null) i = java.time.Instant.now();
        return Timestamp.newBuilder().setSeconds(i.getEpochSecond()).setNanos(i.getNano()).build();
    }
}
