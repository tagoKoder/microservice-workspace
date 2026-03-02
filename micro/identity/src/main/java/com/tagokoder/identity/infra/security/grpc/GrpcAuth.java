package com.tagokoder.identity.infra.security.grpc;

import io.grpc.Status;

public final class GrpcAuth {
  private GrpcAuth() {}

  public static String extractBearerOrThrow() {
    String auth = AuthMetadataServerInterceptor.AUTHORIZATION.get(); // o como lo guardaste
    if (auth == null || auth.isBlank()) {
      throw Status.UNAUTHENTICATED.withDescription("MISSING_AUTHORIZATION").asRuntimeException();
    }

    String a = auth.trim();
    if (!a.regionMatches(true, 0, "Bearer ", 0, 7)) {
      throw Status.UNAUTHENTICATED.withDescription("INVALID_AUTH_SCHEME").asRuntimeException();
    }

    String token = a.substring(7).trim();
    if (token.isBlank()) {
      throw Status.UNAUTHENTICATED.withDescription("EMPTY_BEARER").asRuntimeException();
    }
    return token;
  }

  private static String extractBearer(String auth) {
    if (auth == null) return null;
    String a = auth.trim();
    if (a.regionMatches(true, 0, "Bearer ", 0, 7)) return a.substring(7).trim();
    return null;
  }
}