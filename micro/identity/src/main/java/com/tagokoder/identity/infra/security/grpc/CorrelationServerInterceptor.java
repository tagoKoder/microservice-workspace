package com.tagokoder.identity.infra.security.grpc;

import com.tagokoder.identity.application.AppProps;
import com.tagokoder.identity.infra.security.context.AuthCtx;
import io.grpc.*;

import java.util.UUID;

public class CorrelationServerInterceptor implements ServerInterceptor {

    private static final Metadata.Key<String> X_CORR =
            Metadata.Key.of("x-correlation-id", Metadata.ASCII_STRING_MARSHALLER);

    private static final Metadata.Key<String> X_CHANNEL =
            Metadata.Key.of("x-channel", Metadata.ASCII_STRING_MARSHALLER);

    private static final Metadata.Key<String> X_IDEMP =
            Metadata.Key.of("x-idempotency-key", Metadata.ASCII_STRING_MARSHALLER);

    private static final Metadata.Key<String> X_IP_HASH =
            Metadata.Key.of("x-ip-hash", Metadata.ASCII_STRING_MARSHALLER);

    private static final Metadata.Key<String> X_UA_HASH =
            Metadata.Key.of("x-ua-hash", Metadata.ASCII_STRING_MARSHALLER);

    private final AppProps props;

    public CorrelationServerInterceptor(AppProps props) {
        this.props = props;
    }

    public static String getCorrelationId() {
        return AuthCtx.CORRELATION_ID.get();
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next
    ) {
        String corr = headers.get(X_CORR);
        if (corr == null || corr.isBlank()) corr = UUID.randomUUID().toString();

        String channel = headers.get(X_CHANNEL);
        if (channel == null || channel.isBlank()) channel = props.security().channel();

        String idem = headers.get(X_IDEMP);

        String ipHash = headers.get(X_IP_HASH);
        if (ipHash == null) ipHash = "unknown";

        String uaHash = headers.get(X_UA_HASH);
        if (uaHash == null) uaHash = "unknown";

        Context ctx = Context.current()
                .withValue(AuthCtx.CORRELATION_ID, corr)
                .withValue(AuthCtx.ROUTE_TEMPLATE, call.getMethodDescriptor().getFullMethodName())
                .withValue(AuthCtx.CHANNEL, channel)
                .withValue(AuthCtx.IDEMPOTENCY_KEY, idem)
                .withValue(AuthCtx.IP_HASH, ipHash)
                .withValue(AuthCtx.UA_HASH, uaHash);

        return Contexts.interceptCall(ctx, call, headers, next);
    }
}
