package com.tagokoder.identity.infra.audit;

import java.time.Instant;
import java.util.Map;

public record AuditEventV1(
    String version,              // "1.0"
    Instant timestamp,
    String service,              // "identity"
    String environment,          // "dev|stg|prod"
    String correlation_id,

    String route_template,       // e.g. "grpc:/bank.identity.v1.OidcAuthService/CompleteOidcLogin"
    String action,               // catalog action
    Integer http_status,         // para gRPC puedes mapear: OK=200, UNAUTH=401, PERMISSION=403, etc.

    Actor actor,                 // sin PII
    Map<String, Object> context, // channel, mfa flags, etc (sin PII)
    Map<String, Object> resource,// si aplica
    Decision decision            // SOLO si AVP (en identity normalmente null)
) {
  public record Actor(String subject, String provider) {} // subject = sub del JWT, no email
  public record Decision(String result, String policy_id) {} // opcional
}
