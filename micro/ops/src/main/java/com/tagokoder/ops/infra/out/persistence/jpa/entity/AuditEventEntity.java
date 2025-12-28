package com.tagokoder.ops.infra.out.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@Table(name = "audit_events")
public class AuditEventEntity {
  @Id
  @Column(columnDefinition = "uuid")
  private UUID id;

  @Column(name = "actor_type", nullable = false)
  private String actorType;

  @Column(name = "actor_id", columnDefinition = "uuid")
  private UUID actorId;

  @Column(nullable = false)
  private String action;

  @Column(nullable = false)
  private String resource;

  @Column(name = "resource_id", nullable = false)
  private String resourceId;

  @Column(columnDefinition = "inet")
  private String ip;

  @Column(name = "user_agent")
  private String userAgent;

  @Column(name = "trace_id", nullable = false, columnDefinition = "uuid")
  private UUID traceId;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;
}
