package com.tagokoder.ops.infra.out.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@Table(name = "notification_events")
public class NotificationEventEntity {

  @Id
  @Column(columnDefinition = "uuid")
  private UUID id;

  @Column(nullable = false)
  private String topic;

  @Column(name = "payload_json", nullable = false, columnDefinition = "jsonb")
  private String payloadJson;

  @Column(name = "channel_override")
  private String channelOverride;

  @Column(nullable = false)
  private String status;

  @Column(name = "retry_count", nullable = false)
  private int retryCount;

  @Column(name = "next_retry_at")
  private Instant nextRetryAt;

  @Column(name = "last_error")
  private String lastError;

  @Column(name = "trace_id")
  private UUID traceId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
