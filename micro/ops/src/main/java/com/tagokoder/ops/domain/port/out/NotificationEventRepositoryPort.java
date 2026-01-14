package com.tagokoder.ops.domain.port.out;

import com.tagokoder.ops.domain.model.NotificationEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationEventRepositoryPort {
  UUID insertQueued(NotificationEvent e);
  List<NotificationEvent> fetchNextDueForUpdate(int limit, Instant now); // SKIP LOCKED
  void markSent(UUID id);
  void markFailed(UUID id, int retryCount, Instant nextRetryAt, String lastError, boolean terminal);
}
