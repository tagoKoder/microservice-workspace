package com.tagokoder.account.infra.security.context;

import io.grpc.Context;
import java.util.List;

public final class AuthCtx {
    private AuthCtx() {}

    public static final Context.Key<String> CORRELATION_ID = Context.key("correlation_id");
    public static final Context.Key<AuthPrincipal> PRINCIPAL = Context.key("principal");
    public static final Context.Key<String> ROUTE_TEMPLATE = Context.key("route_template");
    public static final Context.Key<String> IDEMPOTENCY_KEY = Context.key("idempotency_key");
    public static final Context.Key<String> IP_HASH = Context.key("ip_hash");
    public static final Context.Key<String> UA_HASH = Context.key("ua_hash");
    public static final Context.Key<String> CHANNEL = Context.key("channel");

    public record AuthPrincipal(
            String sub,
            String customerIdOrNull,
            List<String> roles,
            boolean mfa
    ) {}
}
