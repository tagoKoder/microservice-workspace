package com.tagokoder.account.infra.out.persistence.jpa.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.annotation.Id;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;


@Entity
@Table(name="account_opening_bonus_grants")
public class OpeningBonusGrantEntity {
  @Id
  @Column(name="idempotency_key")
  private String idempotencyKey;

  @Column(name="account_id", nullable=false)
  private UUID accountId;

  @Column(name="journal_id", nullable=false)
  private String journalId;

  @Column(name="amount", nullable=false)
  private BigDecimal amount;

  @Column(name="currency", nullable=false)
  private String currency;

  @Column(name="created_at", nullable=false)
  private OffsetDateTime createdAt;
}