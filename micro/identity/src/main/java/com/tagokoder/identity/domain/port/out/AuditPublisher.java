package com.tagokoder.identity.domain.port.out;

import com.tagokoder.identity.infra.audit.AuditEventV1;

public interface AuditPublisher {
  void publish(AuditEventV1 evt);
}
