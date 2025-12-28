package com.tagokoder.ops.domain.model;

import java.time.Instant;
import java.util.UUID;

public record NotificationPref(UUID id, UUID customerId, Channel channel, boolean optIn, Instant updatedAt) {}
