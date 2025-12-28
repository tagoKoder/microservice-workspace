package com.tagokoder.ops.infra.out.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@Table(name = "data_access_logs")
public class DataAccessLogEntity {
  @Id
  @Column(columnDefinition = "uuid")
  private UUID id;

  @Column(nullable = false)
  private String subject;

  @Column(nullable = false)
  private String operation;

  @Column(name = "table_name", nullable = false)
  private String tableName;

  @Column(name = "record_id", columnDefinition = "uuid")
  private UUID recordId;

  @Column(nullable = false)
  private String purpose;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

}
