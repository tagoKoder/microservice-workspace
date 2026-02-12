package com.tagokoder.identity.infra.in.grpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tagokoder.identity.domain.port.in.CompleteLoginUseCase;
import com.tagokoder.identity.domain.port.in.GetSessionInfoUseCase;
import com.tagokoder.identity.domain.port.in.LogoutSessionUseCase;
import com.tagokoder.identity.domain.port.in.RefreshSessionUseCase;
import com.tagokoder.identity.domain.port.in.StartLoginUseCase;

import bank.identity.v1.CompleteOidcLoginRequest;
import bank.identity.v1.CompleteOidcLoginResponse;
import bank.identity.v1.GetSessionInfoRequest;
import bank.identity.v1.GetSessionInfoResponse;
import bank.identity.v1.LogoutSessionRequest;
import bank.identity.v1.LogoutSessionResponse;
import bank.identity.v1.OidcAuthServiceGrpc;
import bank.identity.v1.OidcUser;
import bank.identity.v1.RefreshSessionRequest;
import bank.identity.v1.RefreshSessionResponse;
import bank.identity.v1.StartOidcLoginRequest;
import bank.identity.v1.StartOidcLoginResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class OidcAuthGrpcService extends OidcAuthServiceGrpc.OidcAuthServiceImplBase {

  private final StartLoginUseCase startLogin;
  private final CompleteLoginUseCase completeLogin;
  private final RefreshSessionUseCase refreshSessionUseCase;
  private final LogoutSessionUseCase logoutSessionUseCase;
  private final GetSessionInfoUseCase getSessionInfoUseCase;

  private static final Logger log = LoggerFactory.getLogger(OidcAuthGrpcService.class);

  public OidcAuthGrpcService(
      StartLoginUseCase startLogin,
      CompleteLoginUseCase completeLogin,
      RefreshSessionUseCase refreshSessionUseCase,
      LogoutSessionUseCase logoutSessionUseCase,
      GetSessionInfoUseCase getSessionInfoUseCase
  ) {
    this.startLogin = startLogin;
    this.completeLogin = completeLogin;
    this.refreshSessionUseCase = refreshSessionUseCase;
    this.logoutSessionUseCase = logoutSessionUseCase;
    this.getSessionInfoUseCase = getSessionInfoUseCase;
  }

  @Override
  public void getSessionInfo(GetSessionInfoRequest request, StreamObserver<GetSessionInfoResponse> responseObserver) {
    try {
      var sid = java.util.UUID.fromString(request.getSessionId());
      var info = getSessionInfoUseCase.get(sid, request.getIp(), request.getUserAgent());

      var user = OidcUser.newBuilder()
          .setName("")
          .setEmail("")
          // si tienes roles en tu caso de uso, agrégalos aquí:
          // .addAllRoles(info.roles())
          .build();

      var resp = GetSessionInfoResponse.newBuilder()
          .setIdentityId(info.identityId().toString())
          .setSubjectIdOidc(info.subjectIdOidc() != null ? info.subjectIdOidc() : "")
          .setProvider(info.provider() != null ? info.provider() : "")
          .setUserStatus(info.userStatus() != null ? info.userStatus() : "")
          .setSessionExpiresIn(info.expiresInSeconds())
          .setUser(user)
          .setCustomerId(info.customerId() != null ? info.customerId() : "")
          .setAccessToken(info.accessToken() != null ? info.accessToken() : "")
          .setAccessTokenExpiresIn(info.accessTokenExpiresIn())
          .build();

      responseObserver.onNext(resp);
      responseObserver.onCompleted();

    } catch (IllegalArgumentException e) {
      // UUID inválido, etc.
      responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("INVALID_ARGUMENT").asRuntimeException());

    } catch (IllegalStateException e) {
      // aquí estaba el problema: NO throw
      responseObserver.onError(mapSessionException(e));

    } catch (Exception e) {
      log.error("getSessionInfo INTERNAL", e);
      responseObserver.onError(Status.INTERNAL.withDescription("INTERNAL").asRuntimeException());
    }
  }

  @Override
  public void startOidcLogin(StartOidcLoginRequest request, StreamObserver<StartOidcLoginResponse> responseObserver) {
    try {
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

    } catch (Exception e) {
      log.error("startOidcLogin INTERNAL", e);
      responseObserver.onError(Status.INTERNAL.withDescription("INTERNAL").asRuntimeException());
    }
  }

  @Override
  public void completeOidcLogin(CompleteOidcLoginRequest request, StreamObserver<CompleteOidcLoginResponse> responseObserver) {
    try {
      // evita loggear IP cruda en prod; si quieres, loggea hash.
      // log.info("IDENTITY COMPLETE AUTH ip={}", request.getIp());

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

    } catch (Exception e) {
      log.error("completeOidcLogin INTERNAL", e);
      responseObserver.onError(Status.INTERNAL.withDescription("INTERNAL").asRuntimeException());
    }
  }

  @Override
  public void refreshSession(RefreshSessionRequest request, StreamObserver<RefreshSessionResponse> responseObserver) {
    try {
      var sid = java.util.UUID.fromString(request.getSessionId());
      var refreshed = refreshSessionUseCase.refresh(sid, request.getIp(), request.getUserAgent());

      var resp = RefreshSessionResponse.newBuilder()
          .setSessionId(refreshed.sessionId().toString())
          .setSessionExpiresIn(refreshed.expiresInSeconds())
          .setAccessToken(refreshed.accessToken())
          .setAccessTokenExpiresIn(refreshed.accessTokenExpiresIn())
          .build();

      responseObserver.onNext(resp);
      responseObserver.onCompleted();

    } catch (IllegalArgumentException e) {
      responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("INVALID_ARGUMENT").asRuntimeException());

    } catch (IllegalStateException e) {
      responseObserver.onError(mapSessionException(e));

    } catch (Exception e) {
      log.error("refreshSession INTERNAL", e);
      responseObserver.onError(Status.INTERNAL.withDescription("INTERNAL").asRuntimeException());
    }
  }

  @Override
  public void logoutSession(LogoutSessionRequest request, StreamObserver<LogoutSessionResponse> responseObserver) {
    try {
      var sid = java.util.UUID.fromString(request.getSessionId());
      logoutSessionUseCase.logout(sid);

      var resp = LogoutSessionResponse.newBuilder().setSuccess(true).build();
      responseObserver.onNext(resp);
      responseObserver.onCompleted();

    } catch (IllegalArgumentException e) {
      responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("INVALID_ARGUMENT").asRuntimeException());

    } catch (IllegalStateException e) {
      responseObserver.onError(mapSessionException(e));

    } catch (Exception e) {
      log.error("logoutSession INTERNAL", e);
      responseObserver.onError(Status.INTERNAL.withDescription("INTERNAL").asRuntimeException());
    }
  }

  private StatusRuntimeException mapSessionException(IllegalStateException e) {
    String m = e.getMessage() == null ? "" : e.getMessage();

    return switch (m) {
      case "Session not found" ->
          Status.NOT_FOUND.withDescription("SESSION_NOT_FOUND").asRuntimeException();
      case "Session revoked" ->
          Status.UNAUTHENTICATED.withDescription("SESSION_REVOKED").asRuntimeException();
      case "Session expired" ->
          Status.UNAUTHENTICATED.withDescription("SESSION_EXPIRED").asRuntimeException();
      case "Session absolute expiry reached" ->
          Status.FAILED_PRECONDITION.withDescription("SESSION_ABSOLUTE_EXPIRED").asRuntimeException();
      default ->
          Status.UNAUTHENTICATED.withDescription("SESSION_INVALID").asRuntimeException();
    };
  }
}
