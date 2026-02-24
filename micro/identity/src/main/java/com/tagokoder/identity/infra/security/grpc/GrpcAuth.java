package com.tagokoder.identity.infra.security.grpc;

import io.grpc.Status;

public final class GrpcAuth {
  private GrpcAuth() {}

  public static String extractBearerOrThrow() {
    String auth = AuthMetadataServerInterceptor.AUTHORIZATION.get();
    String token = extractBearer(auth);
    if (token == null || token.isBlank()) {
      throw Status.UNAUTHENTICATED.withDescription("Missing token").asRuntimeException();
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