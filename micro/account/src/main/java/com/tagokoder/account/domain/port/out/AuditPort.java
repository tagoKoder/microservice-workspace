package com.tagokoder.account.domain.port.out;

import java.time.Instant;
import java.util.Map;

public interface AuditPort {
    void record(String action, String resource, String resourceId, String actor, Instant at, Map<String, Object> details);
}
