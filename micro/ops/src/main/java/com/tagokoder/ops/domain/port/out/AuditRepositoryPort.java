package com.tagokoder.ops.domain.port.out;

import com.tagokoder.ops.domain.model.AuditEvent;

import java.util.List;
import java.util.UUID;

public interface AuditRepositoryPort {
  void insert(AuditEvent e);
  List<AuditEvent> findByTraceId(UUID traceId, int limit);
}
