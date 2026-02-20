package com.tagokoder.account.infra.security.grpc;

import com.tagokoder.account.infra.config.AppProps;
import com.tagokoder.account.infra.out.audit.AuditPublisher;
import com.tagokoder.account.infra.security.authz.ActionResolver;
import com.tagokoder.account.infra.security.authz.ResourceResolver;
import com.tagokoder.account.infra.security.avp.AvpAuthorizer;
import com.tagokoder.account.infra.security.context.AuthCtx;
import io.grpc.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import software.amazon.awssdk.services.verifiedpermissions.model.AttributeValue;
import software.amazon.awssdk.services.verifiedpermissions.model.Decision;

import java.time.Instant;
import java.util.*;

public class AuthzServerInterceptor implements ServerInterceptor {

    private static final Metadata.Key<String> AUTH =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    private final AppProps props;
    private final JwtDecoder jwtDecoder;
    private final AvpAuthorizer avp;
    private final AuditPublisher audit;
    private final ActionResolver actionResolver;
    private final ResourceResolver resourceResolver;

    public AuthzServerInterceptor(AppProps props,
                                  JwtDecoder jwtDecoder,
                                  AvpAuthorizer avp,
                                  AuditPublisher audit,
                                  ActionResolver actionResolver,
                                  ResourceResolver resourceResolver) {
        this.props = props;
        this.jwtDecoder = jwtDecoder;
        this.avp = avp;
        this.audit = audit;
        this.actionResolver = actionResolver;
        this.resourceResolver = resourceResolver;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        final long started = System.nanoTime();
        final String corrId = AuthCtx.CORRELATION_ID.get();
        final String route = call.getMethodDescriptor().getFullMethodName();
        final var actionDef = actionResolver.resolve(route);

        if (route.startsWith("grpc.health.v1.Health/")) {
        return next.startCall(call, headers);
        }
        // opcional si usas grpcurl / reflection
        if (route.startsWith("grpc.reflection.v1alpha.ServerReflection/")) {
        return next.startCall(call, headers);
        }
        if(actionDef.publicUnauthenticated()){
            return next.startCall(call, headers);
        }

        String auth = headers.get(AUTH);
        String token = extractBearer(auth);

        if (token == null) {
            publishAuthEvent("DENY", null, actionDef.actionId(), route, null, "missing_token");
            call.close(Status.UNAUTHENTICATED.withDescription("Missing token"), new Metadata());
            return new ServerCall.Listener<>() {};
        }

        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(token);
        } catch (Exception e) {
            publishAuthEvent("DENY", null, actionDef.actionId(), route, null, "invalid_token");
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid token"), new Metadata());
            return new ServerCall.Listener<>() {};
        }

        String sub = jwt.getSubject();
        String customerId = claimString(jwt, "custom:customer_id"); // ajusta si tu claim es otro
        List<String> roles = claimStringList(jwt, "cognito:groups");
        boolean mfa = claimStringList(jwt, "amr").contains("mfa");

        var principal = new AuthCtx.AuthPrincipal(sub, customerId, roles, mfa);
        Context ctxWithPrincipal = Context.current().withValue(AuthCtx.PRINCIPAL, principal);

        // Wrap call para auditar al cierre si acción crítica
        ServerCall<ReqT, RespT> callWrapper = new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
            @Override
            public void close(Status status, Metadata trailers) {
                // Si fue crítico, audita ALLOW (si no hubo PERMISSION_DENIED/UNAUTHENTICATED)
                if (actionDef.critical() && status.getCode() == Status.Code.OK) {
                    // ALLOW crítico (la decisión real se setea cuando se autoriza en onMessage)
                    // aquí no repetimos AVP; solo emitimos el "allow-critical" final si ya pasó
                }
                super.close(status, trailers);
            }
        };

        ServerCall.Listener<ReqT> delegate = Contexts.interceptCall(ctxWithPrincipal, callWrapper, headers, next);

        // Para unary: hacemos authz cuando llega el request (onMessage)
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(delegate) {
            @Override
            public void onMessage(ReqT message) {

                var res = resourceResolver.resolve(route, message, principal.customerIdOrNull());

                Map<String, AttributeValue> resourceAttrs = new HashMap<>();
                for (var e : res.attrs().entrySet()) {
                    resourceAttrs.put(e.getKey(), AttributeValue.fromString(String.valueOf(e.getValue())));
                }

                Map<String, AttributeValue> ctxAttrs = new HashMap<>();
                ctxAttrs.put("channel", AttributeValue.fromString(AuthCtx.CHANNEL.get()));
                ctxAttrs.put("ip_hash", AttributeValue.fromString(AuthCtx.IP_HASH.get()));
                ctxAttrs.put("user_agent_hash", AttributeValue.fromString(AuthCtx.UA_HASH.get()));
                if (AuthCtx.IDEMPOTENCY_KEY.get() != null) {
                    ctxAttrs.put("idempotency_key", AttributeValue.fromString(AuthCtx.IDEMPOTENCY_KEY.get()));
                }
                ctxAttrs.put("mfa", AttributeValue.fromString(String.valueOf(principal.mfa())));

                AvpAuthorizer.DecisionResult decision;
                try {
                    decision = avp.authorize(token, actionDef.actionId(), res.type(), res.id(), resourceAttrs, ctxAttrs);
                } catch (Exception e) {
                    publishAuthEvent("DENY", "avp_error", actionDef.actionId(), route, res, e.toString());
                    call.close(Status.PERMISSION_DENIED.withDescription("Authorization error"), new Metadata());
                    return;
                }

                long elapsedMs = (System.nanoTime() - started) / 1_000_000;

                if (decision.decision() != Decision.ALLOW) {
                    publishAuthEvent("DENY", "avp", actionDef.actionId(), route, res, "decision=DENY elapsed_ms=" + elapsedMs);
                    call.close(Status.PERMISSION_DENIED.withDescription("Denied"), new Metadata());
                    return;
                }

                // ALLOW: auditar solo si crítico (o si tú quieres también samplear)
                if (actionDef.critical()) {
                    publishAuthEvent("ALLOW", "avp", actionDef.actionId(), route, res, "elapsed_ms=" + elapsedMs);
                }

                super.onMessage(message);
            }
        };
    }

    private void publishAuthEvent(String outcome,
                                  String decisionSourceOrNull,
                                  String actionId,
                                  String route,
                                  Object resourceOrNull,
                                  String note) {
        String corrId = AuthCtx.CORRELATION_ID.get();
        var base = new HashMap<>(AuditPublisher.base("account", props.env(), corrId));

        var principal = AuthCtx.PRINCIPAL.get();
        Map<String, Object> actor = principal == null ? Map.of() : Map.of(
                "sub", principal.sub(),
                "customer_id", principal.customerIdOrNull(),
                "roles", principal.roles(),
                "mfa", principal.mfa()
        );

        base.put("route_template", route);
        base.put("action", actionId);
        base.put("actor", actor);
        base.put("channel", AuthCtx.CHANNEL.get());
        base.put("ip_hash", AuthCtx.IP_HASH.get());
        base.put("user_agent_hash", AuthCtx.UA_HASH.get());
        base.put("idempotency_key", AuthCtx.IDEMPOTENCY_KEY.get());
        base.put("outcome", outcome);
        base.put("note", note);
        base.put("occurred_at", Instant.now().toString());

        if (decisionSourceOrNull != null) {
            base.put("decision", Map.of(
                    "source", decisionSourceOrNull,
                    "policy_store_id", props.aws().avpPolicyStoreId(),
                    "decision", outcome.equals("ALLOW") ? "ALLOW" : "DENY"
            ));
        }

        if (resourceOrNull instanceof com.tagokoder.account.infra.security.authz.ResourceResolver.ResourceDef r) {
            base.put("resource", Map.of(
                    "type", r.type(),
                    "id", r.id(),
                    "attrs", r.attrs()
            ));
        }

        audit.publish(base);
    }

    private static String extractBearer(String auth) {
        if (auth == null) return null;
        String a = auth.trim();
        if (a.regionMatches(true, 0, "Bearer ", 0, 7)) return a.substring(7).trim();
        return null;
    }

    private static String claimString(Jwt jwt, String name) {
        Object v = jwt.getClaims().get(name);
        return v == null ? null : String.valueOf(v);
    }

    @SuppressWarnings("unchecked")
    private static List<String> claimStringList(Jwt jwt, String name) {
        Object v = jwt.getClaims().get(name);
        if (v instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object x : list) out.add(String.valueOf(x));
            return out;
        }
        return List.of();
    }
}
