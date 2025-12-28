package com.tagokoder.ops.infra.out.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@Table(name = "notification_prefs")
public class NotificationPrefEntity {
  @Id
  @Column(columnDefinition = "uuid")
  private UUID id;

  @Column(name = "customer_id", nullable = false, columnDefinition = "uuid")
  private UUID customerId;

  @Column(nullable = false)
  private String channel;

  @Column(name = "opt_in", nullable = false)
  private boolean optIn;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
