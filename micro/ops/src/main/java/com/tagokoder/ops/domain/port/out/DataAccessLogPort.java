package com.tagokoder.ops.domain.port.out;

import java.time.Instant;
import java.util.UUID;

public interface DataAccessLogPort {
  void log(String subject, String operation, String table, UUID recordId, String purpose, Instant occurredAt);
}
