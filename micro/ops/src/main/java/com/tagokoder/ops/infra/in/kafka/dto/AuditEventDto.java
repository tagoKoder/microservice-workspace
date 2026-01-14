package com.tagokoder.ops.infra.in.kafka.dto;

public record AuditEventDto(
    String actor_type,
    String actor_id,
    String action,
    String resource,
    String resource_id,
    String ip,
    String user_agent,
    String trace_id,
    String occurred_at
) {}
