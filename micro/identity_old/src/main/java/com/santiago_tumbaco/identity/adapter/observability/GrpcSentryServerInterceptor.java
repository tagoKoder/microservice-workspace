package com.santiago_tumbaco.identity.adapter.observability;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import io.grpc.ForwardingServerCall;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;

import io.sentry.Sentry;
import io.sentry.ISentryLifecycleToken;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

@Component
@Order(20)
public class GrpcSentryServerInterceptor implements ServerInterceptor {

  private static final int MAX_REQ_JSON = 16 * 1024;

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call,
      Metadata headers,
      ServerCallHandler<ReqT, RespT> next) {

    // Envolver el ServerCall para interceptar el cierre (server-side)
    ServerCall<ReqT, RespT> wrappedCall = new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
      @Override
      public void close(Status status, Metadata trailers) {
        try {
          if (!status.isOk()) {
            var extras = new HashMap<String, Object>();
            extras.put("grpc.full_method", getMethodDescriptor().getFullMethodName());
            extras.put("grpc.status_code", status.getCode().name());
            extras.put("grpc.md", sanitizeHeaders(headers));
            String peer = peerAddr(this);
            if (peer != null) extras.put("grpc.peer", peer);

            Throwable t = status.asRuntimeException(trailers);
            captureWithOtelContext(t, extras);
          }
        } finally {
          // Siempre delega el cierre
          super.close(status, trailers);
        }
      }
    };

    ServerCall.Listener<ReqT> listener = next.startCall(wrappedCall, headers);
    // Si quieres interceptar mensajes, envuelve el listener:
    return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(listener) { };
  }

    private void captureWithOtelContext(Throwable t, Map<String, Object> extras) {
    try (io.sentry.ISentryLifecycleToken ignored = Sentry.pushScope()) {
        Sentry.configureScope(scope -> {
        // trace/span + baggage
        io.opentelemetry.api.trace.SpanContext sc = io.opentelemetry.api.trace.Span.current().getSpanContext();
        if (sc.isValid()) {
            scope.setTag("trace_id", sc.getTraceId());
            scope.setTag("span_id", sc.getSpanId());
        }
        io.opentelemetry.api.baggage.Baggage bag = io.opentelemetry.api.baggage.Baggage.current();
        String u = bag.getEntryValue("bc.user_id");
        String a = bag.getEntryValue("bc.agency_id");
        if (u != null) scope.setTag("user_id", u);
        if (a != null) scope.setTag("agency_id", a);

        if (extras != null) {
            // 1) Extra plano (Sentry Java requiere String en setExtra)
            extras.forEach((k, v) -> scope.setExtra(k, String.valueOf(v)));  // <-- FIX

            // 2) También como contexto estructurado (opcional, útil para inspección)
            scope.setContexts("grpc", extras);
        }
        });

        Sentry.captureException(t);
    }
    }


  private Map<String, String> sanitizeHeaders(Metadata md) {
    Map<String, String> out = new HashMap<>();
    for (String k : md.keys()) {
      String kl = k.toLowerCase();
      if (kl.endsWith("-bin")) continue;
      if (kl.equals("authorization") || kl.equals("cookie") || kl.equals("set-cookie") || kl.equals("x-api-key")) continue;
      Key<String> key = Key.of(kl, Metadata.ASCII_STRING_MARSHALLER);
      String v = md.get(key);
      if (v != null) out.put(kl, v);
    }
    return out;
  }

  private String peerAddr(ServerCall<?, ?> call) {
    var attrs = call.getAttributes();
    var addr = attrs.get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
    if (addr instanceof InetSocketAddress a) {
      return a.getAddress().getHostAddress() + ":" + a.getPort();
    }
    return null;
  }

  @SuppressWarnings("unused")
  private String reqToSafeJson(Object req) {
    if (req instanceof Message m) {
      try {
        String json = JsonFormat.printer().print(m);
        // Redacta claves sensibles (naïve)
        json = json.replaceAll("(?i)\"(password|token|authorization)\"\\s*:\\s*\"[^\"]*\"", "\"$1\":\"***\"");
        if (json.length() > MAX_REQ_JSON) return json.substring(0, MAX_REQ_JSON) + "...(truncated)";
        return json;
      } catch (Exception ignored) {}
    }
    return null;
  }
}