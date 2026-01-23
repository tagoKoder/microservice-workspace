package com.tagokoder.account.infra.in.grpc;

import org.springframework.stereotype.Component;

import io.grpc.*;

@Component
public class IdempotencyKeyInterceptor implements ServerInterceptor {

  public static final Context.Key<String> IDEMPOTENCY_KEY = Context.key("idempotency-key");
  private static final Metadata.Key<String> HDR =
    Metadata.Key.of("idempotency-key", Metadata.ASCII_STRING_MARSHALLER);

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
    ServerCall<ReqT, RespT> call,
    Metadata headers,
    ServerCallHandler<ReqT, RespT> next
  ) {
    String key = headers.get(HDR);
    Context ctx = Context.current().withValue(IDEMPOTENCY_KEY, key);
    return Contexts.interceptCall(ctx, call, headers, next);
  }
}
