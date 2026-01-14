package com.tagokoder.identity.infra.in.grpc;

import com.tagokoder.identity.domain.port.in.StartRegistrationUseCase;

import bank.identity.v1.*;

import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

@Component
public class OnboardingGrpcService extends OnboardingServiceGrpc.OnboardingServiceImplBase {

  private final StartRegistrationUseCase startRegistration;

  public OnboardingGrpcService(StartRegistrationUseCase startRegistration) {
    this.startRegistration = startRegistration;
  }

  @Override
  public void startRegistration(StartRegistrationRequest request,
                                StreamObserver<StartRegistrationResponse> responseObserver) {

    String occupationType = mapOccupationType(request.getOccupationType());
    byte[] idDocumentFront = request.getIdDocumentFront().toByteArray();
    byte[] selfie = request.getSelfie().toByteArray();
    var res = startRegistration.start(new StartRegistrationUseCase.StartRegistrationCommand(
        request.getChannel(),
        request.getNationalId(),
        LocalDate.parse(request.getNationalIdIssueDate()),
        request.getFingerprintCode(),
        idDocumentFront,
        selfie,
        request.getMonthlyIncome(),
        occupationType,
        request.getEmail(),
        request.getPhone()
    ));

    var createdAtInstant = res.createdAt(); // asumo Instant
    Timestamp createdAt = Timestamp.newBuilder()
        .setSeconds(createdAtInstant.getEpochSecond())
        .setNanos(createdAtInstant.getNano())
        .build();

    var response = StartRegistrationResponse.newBuilder()
        .setRegistrationId(String.valueOf(res.registrationId()))
        .setState(res.state() != null ? res.state() : "")
        .setCreatedAt(createdAt)
        .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  private String mapOccupationType(OccupationType type) {
    if (type == null) return null;
    return switch (type) {
      case OCCUPATION_TYPE_STUDENT -> "student";
      case OCCUPATION_TYPE_EMPLOYEE -> "employee";
      case OCCUPATION_TYPE_SELF_EMPLOYED -> "self_employed";
      case OCCUPATION_TYPE_UNEMPLOYED -> "unemployed";
      case OCCUPATION_TYPE_RETIRED -> "retired";
      default -> null;
    };
  }
}
