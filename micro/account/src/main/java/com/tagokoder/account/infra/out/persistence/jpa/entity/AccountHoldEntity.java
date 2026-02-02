package com.tagokoder.account.infra.out.persistence.jpa.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "account_holds")
@Data
public class AccountHoldEntity {
  @EmbeddedId
  private AccountHoldId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("accountId")
  @JoinColumn(name = "account_id", nullable = false)
  private AccountEntity account;

  @Column(name = "currency", nullable = false)
  private String currency;

  @Column(name="amount", nullable=false)
  private BigDecimal amount;

  @Column(name="status", nullable=false)
  private String status; // reserved|released|settled

  @Column(name="idempotency_key", nullable=false, unique=true)
  private String idempotencyKey;

    @Column(name="created_at", nullable=false)
    private OffsetDateTime createdAt;

    @Column(name="updated_at", nullable=false)
    private OffsetDateTime updatedAt;
}
