package com.tagokoder.ops.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationEvent(
    UUID id,
    String topic,
    Map<String, Object> payload,
    Channel channelOverride,
    String status,
    int retryCount,
    Instant nextRetryAt,
    String lastError,
    UUID traceId,
    Instant createdAt
) {}
