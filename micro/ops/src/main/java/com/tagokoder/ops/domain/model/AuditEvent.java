package com.tagokoder.ops.domain.model;

import java.time.Instant;
import java.util.UUID;

public record AuditEvent(
    UUID id,
    String actorType,
    UUID actorId,
    String action,
    String resource,
    String resourceId,
    String ip,
    String userAgent,
    UUID traceId,
    Instant occurredAt
) {}
