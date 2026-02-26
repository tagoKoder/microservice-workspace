package com.tagokoder.identity.infra.security.grpc;

import com.tagokoder.identity.application.AppProps;
import com.tagokoder.identity.infra.out.audit.AuditPublisher;
import com.tagokoder.identity.infra.security.authz.ActionResolver;
import com.tagokoder.identity.infra.security.authz.ResourceResolver;
import com.tagokoder.identity.infra.security.avp.AvpAuthorizer;
import com.tagokoder.identity.infra.security.context.AuthCtx;
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
        final String route = call.getMethodDescriptor().getFullMethodName();
        final var actionDef = actionResolver.resolve(route);
        final var publicUnauthenticated=actionDef.publicUnauthenticated();

        //System.out.println("Route, ActionDef, publicUnauthenticated: "+route+", "+actionDef+", "+publicUnauthenticated);
        // Infra: health (y opcional reflection) no requieren Bearer
        if (route.startsWith("grpc.health.v1.Health/")) {
        return next.startCall(call, headers);
        }
        // opcional si usas grpcurl / reflection
        if (route.startsWith("grpc.reflection.v1alpha.ServerReflection/")) {
        return next.startCall(call, headers);
        }
        
        // Permite flujos “public” (no bearer) para OIDC/Onboarding
        if (publicUnauthenticated) {
            //System.out.println("In Unauthenticated CLAUSE, ROUTE: "+route);
            return next.startCall(call, headers);
        }

        String token = extractBearer(headers.get(AUTH));
        if (token == null) {
            publishDecision(null, actionDef.actionId(), route, null,
                    "DENY", null, "missing_token", started);
            call.close(Status.UNAUTHENTICATED.withDescription("Missing token"), new Metadata());
            return new ServerCall.Listener<>() {};
        }

        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(token);
        } catch (Exception e) {
            publishDecision(null, actionDef.actionId(), route, null,
                    "DENY", null, "invalid_token", started);
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid token"), new Metadata());
            return new ServerCall.Listener<>() {};
        }

        String sub = jwt.getSubject();
        String customerId = claimString(jwt, "custom:customer_id");
        List<String> roles = claimStringList(jwt, "cognito:groups");
        boolean mfa = claimStringList(jwt, "amr").contains("mfa");

        var principal = new AuthCtx.AuthPrincipal(sub, customerId, roles, mfa);
        Context ctxWithPrincipal = Context.current().withValue(AuthCtx.PRINCIPAL, principal);

        ServerCall.Listener<ReqT> delegate = Contexts.interceptCall(ctxWithPrincipal, call, headers, next);

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(delegate) {
            @Override
            public void onMessage(ReqT message) {

                var res = resourceResolver.resolve(route, message, principal.customerIdOrNull());

                Map<String, AttributeValue> resourceAttrs = new HashMap<>();
                for (var e : res.attrs().entrySet()) {
                    resourceAttrs.put(e.getKey(), AttributeValue.builder().string(String.valueOf(e.getValue())).build());
                }

                Map<String, AttributeValue> ctxAttrs = new HashMap<>();
                ctxAttrs.put("channel", AttributeValue.builder().string(nvl(AuthCtx.CHANNEL.get(), "unknown")).build());
                ctxAttrs.put("ip_hash", AttributeValue.builder().string(nvl(AuthCtx.IP_HASH.get(), "unknown")).build());
                ctxAttrs.put("user_agent_hash", AttributeValue.builder().string(nvl(AuthCtx.UA_HASH.get(), "unknown")).build());
                if (AuthCtx.IDEMPOTENCY_KEY.get() != null) {
                    ctxAttrs.put("idempotency_key", AttributeValue.builder().string(AuthCtx.IDEMPOTENCY_KEY.get()).build());
                }
                ctxAttrs.put("mfa", AttributeValue.builder().booleanValue(principal.mfa()).build());

                AvpAuthorizer.DecisionResult decision;
                try {
                    decision = avp.authorize(token, actionDef.actionId(), res.type(), res.id(), resourceAttrs, ctxAttrs);
                } catch (Exception e) {
                    publishDecision(principal, actionDef.actionId(), route, res,
                            "DENY", "avp_error", e.toString(), started);
                    call.close(Status.PERMISSION_DENIED.withDescription("Authorization error"), new Metadata());
                    return;
                }

                if (decision.decision() != Decision.ALLOW) {
                    publishDecision(principal, actionDef.actionId(), route, res,
                            "DENY", "avp", "decision=DENY", started);
                    call.close(Status.PERMISSION_DENIED.withDescription("Denied"), new Metadata());
                    return;
                }

                // ALLOW: solo si crítico
                if (actionDef.critical()) {
                    publishDecision(principal, actionDef.actionId(), route, res,
                            "ALLOW", "avp", "decision=ALLOW", started);
                }

                super.onMessage(message);
            }
        };
    }

    private void publishDecision(AuthCtx.AuthPrincipal principalOrNull,
                                 String actionId,
                                 String route,
                                 Object resourceOrNull,
                                 String outcome,
                                 String decisionSourceOrNull,
                                 String note,
                                 long startedNano) {

        String corrId = AuthCtx.CORRELATION_ID.get();
        long elapsedMs = (System.nanoTime() - startedNano) / 1_000_000;

        Map<String, Object> ev = audit.base(
                nvl(corrId, "unknown"),
                route,
                actionId
        );

        // actor (sin PII cruda)
        if (principalOrNull != null) {
            ev.put("actor", Map.of(
                    "sub", principalOrNull.sub(),
                    "customer_id", principalOrNull.customerIdOrNull(),
                    "roles", principalOrNull.roles(),
                    "mfa", principalOrNull.mfa()
            ));
        } else {
            ev.put("actor", Map.of());
        }

        ev.put("channel", AuthCtx.CHANNEL.get());
        ev.put("ip_hash", AuthCtx.IP_HASH.get());
        ev.put("user_agent_hash", AuthCtx.UA_HASH.get());
        ev.put("idempotency_key", AuthCtx.IDEMPOTENCY_KEY.get());
        ev.put("outcome", outcome);
        ev.put("note", note);
        ev.put("elapsed_ms", elapsedMs);
        ev.put("at", Instant.now().toString());

        // decision solo si AVP
        if (decisionSourceOrNull != null) {
            ev.put("decision", Map.of(
                    "source", decisionSourceOrNull,
                    "policy_store_id", props.aws().avpPolicyStoreId(),
                    "decision", outcome
            ));
        }

        if (resourceOrNull instanceof com.tagokoder.identity.infra.security.authz.ResourceResolver.ResourceDef r) {
            ev.put("resource", Map.of(
                    "type", r.type(),
                    "id", r.id(),
                    "attrs", r.attrs()
            ));
        }

        audit.publish(ev);
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

    private static String nvl(String v, String def) {
        return (v == null || v.isBlank()) ? def : v;
    }
}
