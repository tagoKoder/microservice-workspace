package com.tagokoder.ops.infra.out.persistence.jpa.adapter;

import com.tagokoder.ops.domain.model.AuditEvent;
import com.tagokoder.ops.domain.port.out.AuditRepositoryPort;
import com.tagokoder.ops.infra.out.persistence.jpa.entity.AuditEventEntity;
import com.tagokoder.ops.infra.out.persistence.jpa.SpringDataAuditJpa;

import java.util.List;
import java.util.UUID;

public class AuditRepositoryAdapter implements AuditRepositoryPort {

  private final SpringDataAuditJpa jpa;

  public AuditRepositoryAdapter(SpringDataAuditJpa jpa) {
    this.jpa = jpa;
  }

  @Override
  public void insert(AuditEvent e) {
    AuditEventEntity ent = new AuditEventEntity();
    ent.setId(e.id());
    ent.setActorType(e.actorType());
    ent.setActorId(e.actorId());
    ent.setAction(e.action());
    ent.setResource(e.resource());
    ent.setResourceId(e.resourceId());
    ent.setIp(e.ip());
    ent.setUserAgent(e.userAgent());
    ent.setTraceId(e.traceId());
    ent.setOccurredAt(e.occurredAt());
    jpa.save(ent);
  }

  @Override
  public List<AuditEvent> findByTraceId(UUID traceId, int limit) {
    // simplificación: top 200; si quieres paginación real, se implementa con Pageable
    return jpa.findTop200ByTraceIdOrderByOccurredAtDesc(traceId).stream()
        .map(e -> new AuditEvent(
            e.getId(),
            e.getActorType(),
            e.getActorId(),
            e.getAction(),
            e.getResource(),
            e.getResourceId(),
            e.getIp(),
            e.getUserAgent(),
            e.getTraceId(),
            e.getOccurredAt()
        ))
        .toList();
  }
}
