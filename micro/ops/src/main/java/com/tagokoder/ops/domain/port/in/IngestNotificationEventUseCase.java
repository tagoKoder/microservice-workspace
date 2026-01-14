package com.tagokoder.ops.domain.port.in;

import java.util.Map;
import java.util.UUID;

public interface IngestNotificationEventUseCase {
  record Command(String topic, Map<String, Object> payload, String channelOverride, UUID traceId) {}
  record Result(boolean accepted, UUID eventId) {}

  Result ingest(Command c);
}
