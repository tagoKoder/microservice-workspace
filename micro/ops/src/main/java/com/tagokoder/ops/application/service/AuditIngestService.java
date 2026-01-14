package com.tagokoder.ops.application.service;

import com.tagokoder.ops.domain.model.AuditEvent;
import com.tagokoder.ops.domain.port.in.IngestAuditEventUseCase;
import com.tagokoder.ops.domain.port.out.AuditRepositoryPort;

import java.util.UUID;

public class AuditIngestService implements IngestAuditEventUseCase {

  private final AuditRepositoryPort repo;

  public AuditIngestService(AuditRepositoryPort repo) {
    this.repo = repo;
  }

  @Override
  public void ingest(Command c) {
    repo.insert(new AuditEvent(
        UUID.randomUUID(),
        c.actorType(),
        c.actorId(),
        c.action(),
        c.resource(),
        c.resourceId(),
        c.ip(),
        c.userAgent(),
        c.traceId(),
        c.occurredAt()
    ));
  }
}
