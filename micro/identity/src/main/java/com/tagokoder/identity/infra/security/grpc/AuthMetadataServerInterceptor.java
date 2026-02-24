package com.tagokoder.identity.infra.security.grpc;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

public class AuthMetadataServerInterceptor implements ServerInterceptor {

  public static final Context.Key<String> AUTHORIZATION =
      Context.key("authorization");

  private static final Metadata.Key<String> AUTH_HEADER =
      Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call,
      Metadata headers,
      ServerCallHandler<ReqT, RespT> next
  ) {
    String auth = headers.get(AUTH_HEADER);
    Context ctx = Context.current().withValue(AUTHORIZATION, auth == null ? "" : auth);
    return Contexts.interceptCall(ctx, call, headers, next);
  }
}