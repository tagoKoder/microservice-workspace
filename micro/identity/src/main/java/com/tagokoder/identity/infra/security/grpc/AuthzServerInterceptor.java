package com.tagokoder.identity.infra.security.grpc;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import com.tagokoder.identity.application.AppProps;
import com.tagokoder.identity.infra.out.audit.AuditPublisher;
import com.tagokoder.identity.infra.security.authz.AuthzMode;
import com.tagokoder.identity.infra.security.authz.IdentityResourceTemplates;
import com.tagokoder.identity.infra.security.authz.RouteAuthzRegistry;
import com.tagokoder.identity.infra.security.avp.AvpAuthorizer;
import com.tagokoder.identity.infra.security.avp.AvpValues;
import com.tagokoder.identity.infra.security.context.AuthCtx;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.ForwardingServerCall;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import software.amazon.awssdk.services.verifiedpermissions.model.AttributeValue;
import software.amazon.awssdk.services.verifiedpermissions.model.Decision;
import software.amazon.awssdk.services.verifiedpermissions.model.EntityIdentifier;
import software.amazon.awssdk.services.verifiedpermissions.model.EntityItem;

public class AuthzServerInterceptor implements ServerInterceptor {

  private static final Logger log = LoggerFactory.getLogger(AuthzServerInterceptor.class);

  private static final Metadata.Key<String> AUTH =
      Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

  private static final String USER_TYPE = "ImaginaryBank::User";

  private final AppProps props;
  private final JwtDecoder jwtDecoder;
  private final AvpAuthorizer avp;
  private final AuditPublisher audit;

  private final RouteAuthzRegistry registry;
  private final IdentityResourceTemplates templates;

  public AuthzServerInterceptor(
      AppProps props,
      JwtDecoder jwtDecoder,
      AvpAuthorizer avp,
      AuditPublisher audit,
      RouteAuthzRegistry registry,
      IdentityResourceTemplates templates
  ) {
    this.props = props;
    this.jwtDecoder = jwtDecoder;
    this.avp = avp;
    this.audit = audit;
    this.registry = registry;
    this.templates = templates;
  }

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

    final long started = System.nanoTime();
    final String route = call.getMethodDescriptor().getFullMethodName();

    // infra bypass
    if (route.startsWith("grpc.health.v1.Health/") ||
        route.startsWith("grpc.reflection.v1alpha.ServerReflection/")) {
      return next.startCall(call, headers);
    }

    var def = registry.get(route);

    // FAIL-CLOSED: no registrada
    if (def == null) {
      log.warn("AUTHZ_DENY_EARLY reason=route_not_registered route={}", route);
      publish("DENY", "route_not_registered", "unknown", route, started);
      call.close(Status.PERMISSION_DENIED.withDescription("Denied"), new Metadata());
      return new ServerCall.Listener<>() {};
    }

    log.debug("AUTHZ route={} action={} mode={} template={}",
        route, def.actionId(), def.mode(), def.resourceTemplate());

    // PUBLIC
    if (def.mode() == AuthzMode.PUBLIC) {
      return next.startCall(call, headers);
    }

    // Token requerido (AUTHN_ONLY/AUTHZ)
    String token = extractBearer(headers.get(AUTH));
    if (token == null) {
      publish("DENY", "missing_token", def.actionId(), route, started);
      call.close(Status.UNAUTHENTICATED.withDescription("Missing token"), new Metadata());
      return new ServerCall.Listener<>() {};
    }

    Jwt jwt;
    try {
      jwt = jwtDecoder.decode(token);
    } catch (Exception e) {
      publish("DENY", "invalid_token", def.actionId(), route, started);
      call.close(Status.UNAUTHENTICATED.withDescription("Invalid token"), new Metadata());
      return new ServerCall.Listener<>() {};
    }

    String principalId = buildPrincipalId(jwt); // pool|sub
    String sub = jwt.getSubject();
    List<String> roles = claimStringList(jwt, "cognito:groups");
    boolean mfa = claimStringList(jwt, "amr").contains("mfa");
    String customerId = claimString(jwt, "custom:customer_id"); // puede venir vacío

    // Siempre setea principal en contexto
    var principal = new AuthCtx.AuthPrincipal(sub, customerId, roles, mfa);
    Context ctxWithPrincipal = Context.current().withValue(AuthCtx.PRINCIPAL, principal);

    // AUTHN_ONLY => no AVP
    if (def.mode() == AuthzMode.AUTHN_ONLY) {
      return Contexts.interceptCall(ctxWithPrincipal, call, headers, next);
    }

    // ---- AUTHZ ----
    final AtomicBoolean closed = new AtomicBoolean(false);
    ServerCall<ReqT, RespT> callSafe = new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
      @Override public void close(Status status, Metadata trailers) {
        if (closed.compareAndSet(false, true)) super.close(status, trailers);
      }
    };

    BiConsumer<Status, String> deny = (st, note) -> {
      log.warn("AUTHZ_DENY note={} route={} action={}", note, route, def.actionId());

      Metadata trailers = new Metadata();
      trailers.put(Metadata.Key.of("x-authz-deny-reason", Metadata.ASCII_STRING_MARSHALLER), note);

      String cid = AuthCtx.CORRELATION_ID.get();
      if (cid != null && !cid.isBlank()) {
        trailers.put(Metadata.Key.of("x-correlation-id", Metadata.ASCII_STRING_MARSHALLER), cid);
      }

      publish("DENY", note, def.actionId(), route, started);
      callSafe.close(st, trailers);
    };

    ServerCall.Listener<ReqT> delegate = Contexts.interceptCall(ctxWithPrincipal, callSafe, headers, next);

    // principal entity
    Map<String, AttributeValue> principalAttrs = new HashMap<>();
    if (customerId != null && !customerId.isBlank()) {
      principalAttrs.put("customer_id", AvpValues.str(customerId));
    }
    principalAttrs.put("roles", AvpValues.setStr(roles));

    EntityIdentifier pid = EntityIdentifier.builder().entityType(USER_TYPE).entityId(principalId).build();
    EntityItem principalEntity = EntityItem.builder().identifier(pid).attributes(principalAttrs).build();

    final String tokenF = token;

    return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(delegate) {
      @Override
      public void onMessage(ReqT message) {

        Map<String, AttributeValue> ctxAttrs = new HashMap<>();
        ctxAttrs.put("channel", AvpValues.str(nvl(AuthCtx.CHANNEL.get(), "unknown")));
        ctxAttrs.put("ip_hash", AvpValues.str(nvl(AuthCtx.IP_HASH.get(), "unknown")));
        ctxAttrs.put("user_agent_hash", AvpValues.str(nvl(AuthCtx.UA_HASH.get(), "unknown")));
        if (AuthCtx.IDEMPOTENCY_KEY.get() != null) {
          ctxAttrs.put("idempotency_key", AvpValues.str(AuthCtx.IDEMPOTENCY_KEY.get()));
        }
        ctxAttrs.put("mfa_verified", AvpValues.bool(mfa));

        IdentityResourceTemplates.Resolved res;
        try {
          res = templates.resolve(def.resourceTemplate(), route, message);
        } catch (Exception e) {
          log.error("AUTHZ resource_resolve_error route={} template={} err={}", route, def.resourceTemplate(), e.toString(), e);
          deny.accept(Status.PERMISSION_DENIED.withDescription("Denied"), "resource_resolve_error");
          return;
        }

        if (res.contextAttrs() != null && !res.contextAttrs().isEmpty()) {
          ctxAttrs.putAll(res.contextAttrs());
        }

        AvpAuthorizer.DecisionResult decision;
        try {
          decision = avp.authorizeWithToken(
              tokenF,
              def.actionId(),
              principalEntity,
              res.resourceEntity(),
              res.extraEntities(),
              ctxAttrs
          );
        } catch (Exception e) {
          log.error("AUTHZ avp_error route={} action={} err={}", route, def.actionId(), e.toString(), e);
          deny.accept(Status.PERMISSION_DENIED.withDescription("Authorization error"), "avp_error");
          return;
        }

        if (decision.decision() != Decision.ALLOW) {
          deny.accept(Status.PERMISSION_DENIED.withDescription("Denied"), "avp_deny");
          return;
        }

        if (def.critical()) {
          publish("ALLOW", "avp_allow", def.actionId(), route, started);
        }

        super.onMessage(message);
      }
    };
  }

  private void publish(String outcome, String note, String actionId, String route, long startedNano) {
    String corrId = AuthCtx.CORRELATION_ID.get();
    long elapsedMs = (System.nanoTime() - startedNano) / 1_000_000;

    Map<String, Object> ev = audit.base(
        nvl(corrId, "unknown"),
        nvl(route, "unknown"),
        nvl(actionId, "unknown")
    );

    ev.put("route_template", route);
    ev.put("action", actionId);
    ev.put("outcome", outcome);
    ev.put("note", note);
    ev.put("elapsed_ms", elapsedMs);
    ev.put("occurred_at", Instant.now().toString());

    // decision info si aplica (mantén consistencia)
    ev.put("decision", Map.of(
        "source", "avp",
        "policy_store_id", nvl(props.aws().avpPolicyStoreId(), "unknown"),
        "decision", outcome
    ));

    try {
      audit.publish(ev);
    } catch (Exception ignored) {
      log.warn("audit_publish_failed", ignored);
    }
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
    if (v instanceof List<?> list) return list.stream().map(String::valueOf).toList();
    return List.of();
  }

  private static String buildPrincipalId(Jwt jwt) {
    String iss = jwt.getIssuer() != null ? jwt.getIssuer().toString() : "";
    String poolId = iss.substring(iss.lastIndexOf('/') + 1);
    return poolId + "|" + jwt.getSubject();
  }

  private static String nvl(String v, String def) {
    return (v == null || v.isBlank()) ? def : v;
  }
}