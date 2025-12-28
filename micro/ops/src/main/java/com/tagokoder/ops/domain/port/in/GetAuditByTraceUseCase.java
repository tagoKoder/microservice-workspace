package com.tagokoder.ops.domain.port.in;

import com.tagokoder.ops.domain.model.AuditEvent;
import java.util.List;
import java.util.UUID;

public interface GetAuditByTraceUseCase {
  List<AuditEvent> get(UUID traceId, int limit);
}
