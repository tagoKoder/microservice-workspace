package com.santiago_tumbaco.identity.config;

import io.grpc.*;

public class MetadataInterceptor implements ServerInterceptor {
  public static final Context.Key<String> AUTH_BEARER = Context.key("auth.bearer");
  private static final Metadata.Key<String> AUTH =
      Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

    String raw = headers.get(AUTH);
    if (raw != null && raw.toLowerCase().startsWith("bearer ")) {
      raw = raw.substring(7);
    }
    Context ctx = Context.current().withValue(AUTH_BEARER, raw);
    return Contexts.interceptCall(ctx, call, headers, next);
  }
}