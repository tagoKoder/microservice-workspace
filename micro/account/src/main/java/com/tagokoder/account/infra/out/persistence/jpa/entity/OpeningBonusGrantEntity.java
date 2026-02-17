package com.tagokoder.account.infra.out.persistence.jpa.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;


@Entity
@Table(name="account_opening_bonus_grants")
@Data
public class OpeningBonusGrantEntity {
  @Id
  @UuidGenerator
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;
  @Column(name="idempotency_key")
  private String idempotencyKey;

  @Column(name="account_id", nullable=false)
  private UUID accountId;

  @Column(name="journal_id", nullable=false)
  private String journalId;

  @Column(name="amount", nullable=false)
  private BigDecimal amount;

  @Column(name="currency", nullable=false, length = 3, columnDefinition = "char(3)")
  private String currency;
  @CreationTimestamp
  @Column(name="created_at", nullable=false)
  private OffsetDateTime createdAt;
}