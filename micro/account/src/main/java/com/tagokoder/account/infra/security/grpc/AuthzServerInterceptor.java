package com.tagokoder.account.infra.security.grpc;

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

import com.tagokoder.account.domain.port.out.IdentityPrincipalPort;
import com.tagokoder.account.infra.config.AppProps;
import com.tagokoder.account.infra.out.audit.AuditPublisher;
import com.tagokoder.account.infra.security.authz.AccountResourceTemplates;
import com.tagokoder.account.infra.security.authz.AuthzMode;
import com.tagokoder.account.infra.security.authz.RouteAuthzRegistry;
import com.tagokoder.account.infra.security.avp.AvpAuthorizer;
import com.tagokoder.account.infra.security.avp.AvpValues;
import com.tagokoder.account.infra.security.context.AuthCtx;

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

  private static final Metadata.Key<String> AUTH =
      Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
    private static final Logger log = LoggerFactory.getLogger(AuthzServerInterceptor.class);

  private static final String USER_TYPE = "ImaginaryBank::User";

  private final AppProps props;
  private final JwtDecoder jwtDecoder;
  private final AvpAuthorizer avp;
  private final AuditPublisher audit;

  private final RouteAuthzRegistry registry;
  private final AccountResourceTemplates templates;
  private final IdentityPrincipalPort principalPort;

  public AuthzServerInterceptor(
      AppProps props,
      JwtDecoder jwtDecoder,
      AvpAuthorizer avp,
      AuditPublisher audit,
      RouteAuthzRegistry registry,
      AccountResourceTemplates templates,
      IdentityPrincipalPort principalPort
  ) {
    this.props = props;
    this.jwtDecoder = jwtDecoder;
    this.avp = avp;
    this.audit = audit;
    this.registry = registry;
    this.templates = templates;
    this.principalPort = principalPort;
  }

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

    final long started = System.nanoTime();
    final String route = call.getMethodDescriptor().getFullMethodName();
    final String corrId = AuthCtx.CORRELATION_ID.get();

    // bypass infra
    if (route.startsWith("grpc.health.v1.Health/") ||
        route.startsWith("grpc.reflection.v1alpha.ServerReflection/")) {
      return next.startCall(call, headers);
    }

    RouteAuthzRegistry reg = registry;
    var def = reg.get(route);
    log.debug("AUTHZ route={} action={} mode={} template={} requireLink={}",
    route, def.actionId(), def.mode(), def.resourceTemplate(), def.requireCustomerLink());



    // PUBLIC: no token, no avp
    if (def.mode() == AuthzMode.PUBLIC) {
      return next.startCall(call, headers);
    }

    // ---- token required for AUTHN_ONLY / AUTHZ ----
    String token = extractBearer(headers.get(AUTH));
    if (token == null) {
      publish("DENY", "missing_token", def.actionId(), route, null, started);
      call.close(Status.UNAUTHENTICATED.withDescription("Missing token"), new Metadata());
      return new ServerCall.Listener<>() {};
    }

    Jwt jwt;
    try {
      jwt = jwtDecoder.decode(token);
    } catch (Exception e) {
      publish("DENY", "invalid_token", def.actionId(), route, null, started);
      call.close(Status.UNAUTHENTICATED.withDescription("Invalid token"), new Metadata());
      return new ServerCall.Listener<>() {};
    }

    // principal id AVP (pool|sub)
    String principalId = buildAvpPrincipalId(jwt);
    String sub = jwt.getSubject();

    // roles + mfa (señal, NO obligatoria para pago)
    List<String> roles = claimStringList(jwt, "cognito:groups");
    boolean mfa = claimStringList(jwt, "amr").contains("mfa");

    // customer_id: claim o identity fallback (solo si hace falta)
    String customerId = claimString(jwt, "custom:customer_id");
    if ((customerId == null || customerId.isBlank()) || def.requireCustomerLink()) {
      try {
        var p = principalPort.resolvePrincipal(token, def.requireCustomerLink(), def.actionId());
        if (p.customerId() != null && !p.customerId().isBlank()) customerId = p.customerId();
        if (p.roles() != null && !p.roles().isEmpty()) roles = p.roles();
        log.debug("AUTHZ principal sub={} principalId={} customerId={} roles={} mfa={} token_iss={}",
            sub, principalId, customerId, roles, mfa, (jwt.getIssuer() == null ? "" : jwt.getIssuer().toString()));
      } catch (io.grpc.StatusRuntimeException sre) {
        log.warn("AUTHZ principalPort.resolvePrincipal FAILED route={} status={} desc={}",
            route, sre.getStatus().getCode(), sre.getStatus().getDescription(), sre);
    } 
      catch (Exception e) {
        log.warn("AUTHZ principalPort.resolvePrincipal FAILED route={} err={}",
            route, e.toString(), e);
        // si requireCustomerLink=true y falla => DENY
        if (def.requireCustomerLink() && (customerId == null || customerId.isBlank())) {
        publish("DENY", "customer_link_missing", def.actionId(), route, null, started);
        call.close(Status.PERMISSION_DENIED.withDescription("Denied"), new Metadata());
        return new ServerCall.Listener<>() {};
        }
      }
    }
    if (def.requireCustomerLink() && (customerId == null || customerId.isBlank())) {
    log.warn("AUTHZ_DENY_EARLY reason=customer_link_missing route={} action={}", route, def.actionId());
    publish("DENY", "customer_link_missing", def.actionId(), route, null, started);
    call.close(Status.PERMISSION_DENIED.withDescription("Denied"), new Metadata());
    return new ServerCall.Listener<>() {};
    }
    final String tokenF = token;
    final String principalIdF = principalId;
    final String subF = sub;
    final String customerIdF = customerId;              // <-- final copy
    final List<String> rolesF = List.copyOf(roles);     // <-- final copy
    final boolean mfaF = mfa;

    // AUTHN_ONLY: solo token válido, sin AVP
    if (def.mode() == AuthzMode.AUTHN_ONLY) {
      var principal = new AuthCtx.AuthPrincipal(sub, customerId, roles, mfa);
      Context ctxWithPrincipal = Context.current().withValue(AuthCtx.PRINCIPAL, principal);
      return Contexts.interceptCall(ctxWithPrincipal, call, headers, next);
    }

    // ---- AUTHZ path ----
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

    publish("DENY", note, def.actionId(), route, null, started);
    callSafe.close(st, trailers);
    };

    var principal = new AuthCtx.AuthPrincipal(sub, customerId, roles, mfa);
    Context ctxWithPrincipal = Context.current().withValue(AuthCtx.PRINCIPAL, principal);
    ServerCall.Listener<ReqT> delegate = Contexts.interceptCall(ctxWithPrincipal, callSafe, headers, next);

    // construye principal entity
    Map<String, AttributeValue> principalAttrs = new HashMap<>();
    if (customerId != null && !customerId.isBlank()) {
      principalAttrs.put("customer_id", AvpValues.str(customerId));
    }
    principalAttrs.put("roles", AvpValues.setStr(roles));

    EntityIdentifier pid = EntityIdentifier.builder().entityType(USER_TYPE).entityId(principalId).build();
    EntityItem principalEntity = EntityItem.builder().identifier(pid).attributes(principalAttrs).build();

    return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(delegate) {
      @Override
      public void onMessage(ReqT message) {

        // context attrs estándar
        Map<String, AttributeValue> ctxAttrs = new HashMap<>();
        if (customerIdF != null && !customerIdF.isBlank()) {
        ctxAttrs.put("customer_id", AvpValues.str(customerIdF));
        }
        ctxAttrs.put("channel", AvpValues.str(nvl(AuthCtx.CHANNEL.get(), "unknown")));
        ctxAttrs.put("ip_hash", AvpValues.str(nvl(AuthCtx.IP_HASH.get(), "unknown")));
        ctxAttrs.put("user_agent_hash", AvpValues.str(nvl(AuthCtx.UA_HASH.get(), "unknown")));
        if (AuthCtx.IDEMPOTENCY_KEY.get() != null) {
          ctxAttrs.put("idempotency_key", AvpValues.str(AuthCtx.IDEMPOTENCY_KEY.get()));
        }
        ctxAttrs.put("mfa_verified", AvpValues.bool(principal.mfa())); // SOLO señal, NO policy obligatoria

        AccountResourceTemplates.PrincipalData p =
            new AccountResourceTemplates.PrincipalData(principalId, customerIdF, rolesF, mfaF);

        AccountResourceTemplates.Resolved res;
        try {
          res = templates.resolve(def.resourceTemplate(), route, message, p);
        } catch (Exception e) {
          deny.accept(Status.PERMISSION_DENIED.withDescription("Denied"), "resource_resolve_error");
          return;
        }

        // merge context attrs si template agrega algo
        if (res.contextAttrs() != null && !res.contextAttrs().isEmpty()) {
          ctxAttrs.putAll(res.contextAttrs());
        }

        AvpAuthorizer.DecisionResult decision;
        log.debug("AUTHZ AVP_REQUEST route={} action={} ctx.customer_id={} resourceType={} resourceId={} resourceAttrs={}",
            route,
            def.actionId(),
            customerIdF,
            res.resourceEntity().identifier().entityType(),
            res.resourceEntity().identifier().entityId(),
            res.resourceEntity().attributes()
        );

        log.debug("AUTHZ AVP_CONTEXT keys={}", ctxAttrs.keySet());
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
          deny.accept(Status.PERMISSION_DENIED.withDescription("Authorization error"), "avp_error");
          return;
        }

        log.debug("AUTHZ AVP_RESPONSE decision={} determiningPolicies={}",
            decision.decision(),
            decision.determiningPolicies()
        );

        if (decision.decision() != Decision.ALLOW) {
        log.warn("AUTHZ_DENY route={} action={} ctx.customer_id={} resource.owner_customer_id={} resourceId={} policies={}",
            route,
            def.actionId(),
            customerIdF,
            (res.resourceEntity().attributes().get("owner_customer_id") == null ? null : res.resourceEntity().attributes().get("owner_customer_id").string()),
            res.resourceEntity().identifier().entityId(),
            decision.determiningPolicies()
        );
        }

        if (def.critical()) {
          publish("ALLOW", "avp_allow", def.actionId(), route, null, started);
        }

        super.onMessage(message);
      }
    };
  }

  private void publish(String outcome, String note, String actionId, String route, Object resOrNull, long startedNano) {
    String corrId = AuthCtx.CORRELATION_ID.get();
    long elapsedMs = (System.nanoTime() - startedNano) / 1_000_000;

    var base = new HashMap<>(AuditPublisher.base("account", props.env(), corrId));
    var principal = AuthCtx.PRINCIPAL.get();

    base.put("route_template", route);
    base.put("action", actionId);
    base.put("outcome", outcome);
    base.put("note", note);
    base.put("elapsed_ms", elapsedMs);
    base.put("occurred_at", Instant.now().toString());

    if (principal != null) {
      base.put("actor", Map.of(
          "sub", principal.sub(),
          "customer_id", principal.customerIdOrNull(),
          "roles", principal.roles(),
          "mfa", principal.mfa()
      ));
    }

    base.put("decision", Map.of(
        "source", "avp",
        "policy_store_id", props.aws().avpPolicyStoreId(),
        "decision", outcome
    ));

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
    if (v instanceof List<?> list) return list.stream().map(String::valueOf).toList();
    return List.of();
  }

  private static String buildAvpPrincipalId(Jwt jwt) {
    String iss = jwt.getIssuer() != null ? jwt.getIssuer().toString() : "";
    String poolId = iss.substring(iss.lastIndexOf('/') + 1);
    return poolId + "|" + jwt.getSubject();
  }

  private static String nvl(String v, String def) {
    return (v == null || v.isBlank()) ? def : v;
  }
}