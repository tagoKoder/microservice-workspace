package com.tagokoder.identity.infra.in.grpc;

import org.springframework.stereotype.Component;

import com.tagokoder.identity.application.service.SessionService;
import com.tagokoder.identity.domain.port.in.CompleteLoginUseCase;
import com.tagokoder.identity.domain.port.in.GetSessionInfoUseCase;
import com.tagokoder.identity.domain.port.in.LogoutSessionUseCase;
import com.tagokoder.identity.domain.port.in.RefreshSessionUseCase;
import com.tagokoder.identity.domain.port.in.StartLoginUseCase;

import bank.identity.v1.*;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class OidcAuthGrpcService extends OidcAuthServiceGrpc.OidcAuthServiceImplBase {

  private final StartLoginUseCase startLogin;
  private final CompleteLoginUseCase completeLogin;
  private final RefreshSessionUseCase refreshSessionUseCase;
  private final LogoutSessionUseCase logoutSessionUseCase;
  private final GetSessionInfoUseCase getSessionInfoUseCase;
  private static final Logger log = LoggerFactory.getLogger(OidcAuthGrpcService.class);
  public OidcAuthGrpcService(StartLoginUseCase startLogin, CompleteLoginUseCase completeLogin, 
    RefreshSessionUseCase refreshSessionUseCase, LogoutSessionUseCase logoutSessionUseCase, 
    GetSessionInfoUseCase getSessionInfoUseCase) {
    this.startLogin = startLogin;
    this.completeLogin = completeLogin;
    this.refreshSessionUseCase = refreshSessionUseCase;
    this.logoutSessionUseCase = logoutSessionUseCase;
    this.getSessionInfoUseCase = getSessionInfoUseCase;
  }

  @Override
  public void getSessionInfo(GetSessionInfoRequest request, StreamObserver<GetSessionInfoResponse> responseObserver) {
      var sid = java.util.UUID.fromString(request.getSessionId());
      var info = getSessionInfoUseCase.get(sid, request.getIp(), request.getUserAgent());

      var user = OidcUser.newBuilder()
              .setName("")     // recomendado: si quieres esto, persiste snapshot en DB o en BFF
              .setEmail("")
              .build();

      var resp = GetSessionInfoResponse.newBuilder()
              .setIdentityId(info.identityId().toString())
              .setSubjectIdOidc(info.subjectIdOidc() != null ? info.subjectIdOidc() : "")
              .setProvider(info.provider() != null ? info.provider() : "")
              .setUserStatus(info.userStatus() != null ? info.userStatus() : "")
              .setSessionExpiresIn(info.expiresInSeconds())
              .setUser(user)
              .setCustomerId(info.customerId() != null ? info.customerId() : "")
              .build();

      responseObserver.onNext(resp);
      responseObserver.onCompleted();
  }


  @Override
  public void startOidcLogin(StartOidcLoginRequest request,
                            StreamObserver<StartOidcLoginResponse> responseObserver) {

    var res = startLogin.start(new StartLoginUseCase.StartLoginCommand(
        request.getChannel(),
        request.getRedirectAfterLogin()
    ));

    var response = StartOidcLoginResponse.newBuilder()
        .setAuthorizationUrl(res.authorizationUrl())
        .setState(res.state())
        .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void completeOidcLogin(CompleteOidcLoginRequest request,
                               StreamObserver<CompleteOidcLoginResponse> responseObserver) {

    log.info("IDENTITY CATCHING IP ON COMPLETE AUHT: "+request.getIp());
    var res = completeLogin.complete(new CompleteLoginUseCase.CompleteLoginCommand(
        request.getCode(),
        request.getState(),
        request.getIp(),
        request.getUserAgent(),
        request.getChannel()
    ));

    var user = OidcUser.newBuilder()
        .setName(res.name() != null ? res.name() : "")
        .setEmail(res.email() != null ? res.email() : "")
        .addAllRoles(res.groups() != null ? res.groups() : java.util.List.of())
        .build();

  var response = CompleteOidcLoginResponse.newBuilder()
      .setIdentityId(String.valueOf(res.identityId()))
      .setSubjectIdOidc(res.subjectIdOidc() != null ? res.subjectIdOidc() : "")
      .setProvider(res.provider() != null ? res.provider() : "")
      .setUser(user)
      .setSessionId(String.valueOf(res.sessionId()))
      .setSessionExpiresIn(res.expiresIn())
      .setRedirectAfterLogin(res.redirectAfterLogin() != null ? res.redirectAfterLogin() : "")
      .build();


    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }


  @Override
  public void refreshSession(RefreshSessionRequest request,
                             StreamObserver<RefreshSessionResponse> responseObserver) {
    var sid = java.util.UUID.fromString(request.getSessionId());
    var refreshed = refreshSessionUseCase.refresh(sid, request.getIp(), request.getUserAgent());
    var resp = RefreshSessionResponse.newBuilder()
        .setSessionId(refreshed.sessionId().toString())
        .setSessionExpiresIn(refreshed.expiresInSeconds())
        .build();
    responseObserver.onNext(resp);
    responseObserver.onCompleted();
  }

  @Override
  public void logoutSession(LogoutSessionRequest request,
                            StreamObserver<LogoutSessionResponse> responseObserver) {
    var sid = java.util.UUID.fromString(request.getSessionId());
    logoutSessionUseCase.logout(sid);

    var resp = LogoutSessionResponse.newBuilder()
        .setSuccess(true)
        .build();
    responseObserver.onNext(resp);
    responseObserver.onCompleted();
  }
}
