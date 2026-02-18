package com.tagokoder.identity.infra.in.grpc.validation;

import bank.identity.v1.*;
import com.tagokoder.identity.domain.port.in.*;

import java.util.UUID;

import static com.tagokoder.identity.infra.in.grpc.validation.GrpcValidation.*;

public final class OidcAuthGrpcValidators {
  private OidcAuthGrpcValidators() {}

  public static StartLoginUseCase.StartLoginCommand toStartLoginCommand(StartOidcLoginRequest req) {
    String channel = requireChannel(req.getChannel());

    String redirect = req.getRedirectAfterLogin();
    if (redirect == null || redirect.isBlank()) redirect = "/app"; // default seguro
    redirect = requireSafeRedirectPath(redirect);

    return new StartLoginUseCase.StartLoginCommand(channel, redirect);
  }

  public static CompleteLoginUseCase.CompleteLoginCommand toCompleteLoginCommand(CompleteOidcLoginRequest req) {
    String code = requireOidcCode(req.getCode());
    String state = requireOidcState(req.getState());
    String channel = requireChannel(req.getChannel());

    // Estos deberían venir por context/interceptor; si igual vienen aquí, guard defensivo
    String ip = optionalClientHint(req.getIp(), "ip", 128);
    String ua = optionalClientHint(req.getUserAgent(), "user_agent", 256);

    return new CompleteLoginUseCase.CompleteLoginCommand(code, state, ip, ua, channel);
  }

  public record SessionClientIn(UUID sessionId, String ip, String userAgent) {}

  public static SessionClientIn toSessionClientIn(RefreshSessionRequest req) {
    UUID sid = requireUuid(req.getSessionId(), "session_id");
    String ip = optionalClientHint(req.getIp(), "ip", 128);
    String ua = optionalClientHint(req.getUserAgent(), "user_agent", 256);
    return new SessionClientIn(sid, ip, ua);
  }

  public static SessionClientIn toSessionClientIn(GetSessionInfoRequest req) {
    UUID sid = requireUuid(req.getSessionId(), "session_id");
    String ip = optionalClientHint(req.getIp(), "ip", 128);
    String ua = optionalClientHint(req.getUserAgent(), "user_agent", 256);
    return new SessionClientIn(sid, ip, ua);
  }

  public static UUID toSessionId(LogoutSessionRequest req) {
    return requireUuid(req.getSessionId(), "session_id");
  }
}