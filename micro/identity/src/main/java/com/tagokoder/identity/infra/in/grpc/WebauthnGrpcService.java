package com.tagokoder.identity.infra.in.grpc;

import org.springframework.stereotype.Component;

import com.tagokoder.identity.domain.port.in.WebauthnMfaUseCase;

import bank.identity.v1.WebauthnServiceGrpc;
import bank.identity.v1.BeginWebauthnRegistrationRequest;
import bank.identity.v1.BeginWebauthnRegistrationResponse;
import bank.identity.v1.FinishWebauthnRegistrationRequest;
import bank.identity.v1.FinishWebauthnRegistrationResponse;
import bank.identity.v1.BeginWebauthnAssertionRequest;
import bank.identity.v1.BeginWebauthnAssertionResponse;
import bank.identity.v1.FinishWebauthnAssertionRequest;
import bank.identity.v1.FinishWebauthnAssertionResponse;

import io.grpc.stub.StreamObserver;

@Component
public class WebauthnGrpcService extends WebauthnServiceGrpc.WebauthnServiceImplBase {

    private final WebauthnMfaUseCase webauthn;

    public WebauthnGrpcService(WebauthnMfaUseCase webauthn) {
        this.webauthn = webauthn;
    }

    @Override
    public void beginWebauthnRegistration(BeginWebauthnRegistrationRequest request,
                                         StreamObserver<BeginWebauthnRegistrationResponse> responseObserver) {
        var identityId = java.util.UUID.fromString(request.getIdentityId());

        var res = webauthn.beginRegistration(
            new WebauthnMfaUseCase.BeginRegistrationCommand(identityId, request.getDeviceName())
        );

        var resp = BeginWebauthnRegistrationResponse.newBuilder()
            .setRequestId(res.requestId())
            .setOptionsJson(res.optionsJson())
            .build();

        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    @Override
    public void finishWebauthnRegistration(FinishWebauthnRegistrationRequest request,
                                          StreamObserver<FinishWebauthnRegistrationResponse> responseObserver) {
        webauthn.finishRegistration(
            new WebauthnMfaUseCase.FinishRegistrationCommand(request.getRequestId(), request.getCredentialJson())
        );

        var resp = FinishWebauthnRegistrationResponse.newBuilder()
            .setSuccess(true)
            .build();

        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    @Override
    public void beginWebauthnAssertion(BeginWebauthnAssertionRequest request,
                                      StreamObserver<BeginWebauthnAssertionResponse> responseObserver) {
        var identityId = java.util.UUID.fromString(request.getIdentityId());
        var sessionId  = java.util.UUID.fromString(request.getSessionId());

        var res = webauthn.beginAssertion(
            new WebauthnMfaUseCase.BeginAssertionCommand(identityId, sessionId)
        );

        var resp = BeginWebauthnAssertionResponse.newBuilder()
            .setRequestId(res.requestId())
            .setOptionsJson(res.optionsJson())
            .build();

        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    @Override
    public void finishWebauthnAssertion(FinishWebauthnAssertionRequest request,
                                       StreamObserver<FinishWebauthnAssertionResponse> responseObserver) {
        var sessionId = java.util.UUID.fromString(request.getSessionId());

        webauthn.finishAssertion(
            new WebauthnMfaUseCase.FinishAssertionCommand(sessionId, request.getRequestId(), request.getCredentialJson())
        );

        var resp = FinishWebauthnAssertionResponse.newBuilder()
            .setSuccess(true)
            .build();

        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }
}
