package com.tagokoder.account.infra.out.persistence.jpa.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "idempotency_records")
@Data
public class IdempotencyRecordEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "key", nullable = false, unique = true, length = 255)
  private String key;

  @Column(name = "operation", nullable = false)
  private String operation;

  @Column(name = "response_json", nullable = false, columnDefinition = "jsonb")
  private String responseJson;

  @Column(name = "status_code", nullable = false)
  private int statusCode;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;
}
