package com.tagokoder.ops.application.service;

import com.tagokoder.ops.domain.model.AuditEvent;
import com.tagokoder.ops.domain.model.NotificationPref;
import com.tagokoder.ops.domain.port.in.GetAuditByTraceUseCase;
import com.tagokoder.ops.domain.port.in.GetNotificationPrefsUseCase;
import com.tagokoder.ops.domain.port.out.AuditRepositoryPort;
import com.tagokoder.ops.domain.port.out.NotificationPrefRepositoryPort;

import java.util.List;
import java.util.UUID;

public class QueryServices implements GetNotificationPrefsUseCase, GetAuditByTraceUseCase {

  private final NotificationPrefRepositoryPort prefs;
  private final AuditRepositoryPort audit;

  public QueryServices(NotificationPrefRepositoryPort prefs, AuditRepositoryPort audit) {
    this.prefs = prefs;
    this.audit = audit;
  }

  @Override
  public List<NotificationPref> get(UUID customerId) {
    return prefs.findByCustomer(customerId);
  }

  @Override
  public List<AuditEvent> get(UUID traceId, int limit) {
    return audit.findByTraceId(traceId, limit <= 0 ? 200 : Math.min(limit, 1000));
  }
}
