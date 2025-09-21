package com.santiago_tumbaco.identity.adapter.observability;


import io.grpc.ForwardingServerCall;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.Metadata.Key;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;

import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class GrpcOtelServerInterceptor implements ServerInterceptor {

  private final Tracer tracer;
  private final OpenTelemetry otel;

  public GrpcOtelServerInterceptor(OpenTelemetry otel) {
    this.otel = otel;
    this.tracer = otel.getTracer("identity-service");
  }

  private static final TextMapGetter<Metadata> GETTER = new TextMapGetter<>() {
    @Override public Iterable<String> keys(Metadata carrier) { return carrier.keys(); }
    @Override public String get(Metadata carrier, String key) {
      if (carrier == null) return null;
      Key<String> k = Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
      return carrier.get(k);
    }
  };

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

    // Extraer contexto W3C (tracecontext + baggage) desde los metadatos gRPC
    Context extracted = otel.getPropagators().getTextMapPropagator()
        .extract(Context.current(), headers, GETTER);

    Span span = tracer.spanBuilder(call.getMethodDescriptor().getFullMethodName())
        .setSpanKind(SpanKind.SERVER)
        .setParent(extracted)
        .startSpan();

    Context withSpan = extracted.with(span);
    Scope scope = withSpan.makeCurrent();

    // Enriquecer MDC para logs JSON
    putMdcFromSpanAndBaggage(span, Baggage.fromContext(withSpan));

    // Envolver el ServerCall para interceptar el cierre correcto (server-side)
    ServerCall<ReqT, RespT> wrappedCall = new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
      @Override
      public void close(Status status, Metadata trailers) {
        try {
          if (!status.isOk()) {
            // Registrar error en el span y marcar estado
            super.close(status, trailers);
            span.recordException(status.asRuntimeException(trailers));
            span.setStatus(StatusCode.ERROR, status.getDescription());
          } else {
            super.close(status, trailers);
          }
        } finally {
          span.end();
          clearMdc();
          scope.close();
        }
      }
    };

    ServerCall.Listener<ReqT> listener = next.startCall(wrappedCall, headers);

    // Puedes envolver el listener si quieres añadir lógica en onHalfClose/onMessage
    return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(listener) { };
  }

  private void putMdcFromSpanAndBaggage(Span span, Baggage bag) {
    SpanContext sc = span.getSpanContext();
    if (sc.isValid()) {
      MDC.put("trace_id", sc.getTraceId());
      MDC.put("span_id", sc.getSpanId());
    }
    var u = bag.getEntryValue("bc.user_id");
    var a = bag.getEntryValue("bc.agency_id");
    if (u != null && !u.isEmpty()) MDC.put("user_id", u);
    if (a != null && !a.isEmpty()) MDC.put("agency_id", a);
  }

  private void clearMdc() {
    MDC.remove("trace_id");
    MDC.remove("span_id");
    MDC.remove("user_id");
    MDC.remove("agency_id");
  }
}
