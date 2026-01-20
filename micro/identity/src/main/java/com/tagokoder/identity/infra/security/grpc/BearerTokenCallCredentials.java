package com.tagokoder.identity.infra.security.grpc;

import java.util.concurrent.Executor;

import io.grpc.*;

public final class BearerTokenCallCredentials extends CallCredentials {
  private final String token; // sin "Bearer " o con, tú decides

  public BearerTokenCallCredentials(String token) { this.token = token; }

  @Override
  public void applyRequestMetadata(RequestInfo arg0, Executor arg1, MetadataApplier arg2) {
    arg1.execute(() -> {
      try {
        Metadata headers = new Metadata();
        Metadata.Key<String> AUTH = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
        String v = token.startsWith("Bearer ") ? token : ("Bearer " + token);
        headers.put(AUTH, v);
        arg2.apply(headers);
      } catch (Throwable t) {
        arg2.fail(Status.UNAUTHENTICATED.withCause(t));
      }
    });
  }

  @Override public void thisUsesUnstableApi() {}

}