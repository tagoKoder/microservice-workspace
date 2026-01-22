package com.tagokoder.account.infra.out.persistence.jpa.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "inbox_events")
@Data
public class InboxEventEntity {

  @Id
  @Column(name = "event_id", length = 128, nullable = false)
  private String eventId;

  @Column(name = "event_type", nullable = false)
  private String eventType;

  @Column(name = "received_at", nullable = false)
  private OffsetDateTime receivedAt;

  @Column(name = "processed_at")
  private OffsetDateTime processedAt;

  @Column(name = "status", nullable = false)
  private String status; // received|processed|failed

  @Column(name = "error")
  private String error;
}
