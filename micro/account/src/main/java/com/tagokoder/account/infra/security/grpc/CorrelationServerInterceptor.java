package com.tagokoder.account.infra.security.grpc;

import com.tagokoder.account.infra.config.AppProps;
import com.tagokoder.account.infra.security.context.AuthCtx;
import com.tagokoder.account.infra.security.util.Hashing;
import io.grpc.*;

import java.util.UUID;

public class CorrelationServerInterceptor implements ServerInterceptor {

    private static final Metadata.Key<String> CORR =
            Metadata.Key.of("x-correlation-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> IDEMP =
            Metadata.Key.of("idempotency-key", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> XFF =
            Metadata.Key.of("x-forwarded-for", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> UA =
            Metadata.Key.of("user-agent", Metadata.ASCII_STRING_MARSHALLER);

    private final AppProps props;

    public CorrelationServerInterceptor(AppProps props) {
        this.props = props;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        String cid = headers.get(CORR);
        if (cid == null || cid.isBlank()) cid = UUID.randomUUID().toString();

        String idk = headers.get(IDEMP);
        String ipHash = Hashing.sha256Hex(props.security().hashSalt(), headers.get(XFF));
        String uaHash = Hashing.sha256Hex(props.security().hashSalt(), headers.get(UA));

        Context ctx = Context.current()
                .withValue(AuthCtx.CORRELATION_ID, cid)
                .withValue(AuthCtx.ROUTE_TEMPLATE, call.getMethodDescriptor().getFullMethodName())
                .withValue(AuthCtx.IDEMPOTENCY_KEY, idk)
                .withValue(AuthCtx.IP_HASH, ipHash)
                .withValue(AuthCtx.UA_HASH, uaHash)
                .withValue(AuthCtx.CHANNEL, props.security().channel());

        return Contexts.interceptCall(ctx, call, headers, next);
    }
}
