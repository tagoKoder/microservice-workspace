package com.tagokoder.account.infra.security.grpc;

import java.util.concurrent.Executor;
import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;

public final class BearerTokenCallCredentials extends CallCredentials {
  private final String token;

  public BearerTokenCallCredentials(String token) { this.token = token; }

  @Override
  public void applyRequestMetadata(RequestInfo info, Executor appExecutor, MetadataApplier applier) {
    appExecutor.execute(() -> {
      try {
        Metadata headers = new Metadata();
        Metadata.Key<String> AUTH = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
        String v = token.startsWith("Bearer ") ? token : ("Bearer " + token);
        headers.put(AUTH, v);
        applier.apply(headers);
      } catch (Throwable t) {
        applier.fail(Status.UNAUTHENTICATED.withCause(t));
      }
    });
  }

  @Override public void thisUsesUnstableApi() {}
}