package com.tagokoder.ops.infra.out.persistence.jpa;

import com.tagokoder.ops.infra.out.persistence.jpa.entity.AuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataAuditJpa extends JpaRepository<AuditEventEntity, UUID> {
  List<AuditEventEntity> findTop200ByTraceIdOrderByOccurredAtDesc(UUID traceId);
}
