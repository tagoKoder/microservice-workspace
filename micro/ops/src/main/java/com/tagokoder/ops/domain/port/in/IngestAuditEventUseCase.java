package com.tagokoder.ops.domain.port.in;

import java.time.Instant;
import java.util.UUID;

public interface IngestAuditEventUseCase {
  record Command(
      String actorType, UUID actorId, String action, String resource, String resourceId,
      String ip, String userAgent, UUID traceId, Instant occurredAt
  ) {}
  void ingest(Command c);
}
