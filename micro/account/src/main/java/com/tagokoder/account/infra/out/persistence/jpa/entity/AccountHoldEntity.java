package com.tagokoder.account.infra.out.persistence.jpa.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.Data;

@Entity(name = "account_holds")
@Data
public class AccountHoldEntity {
  @EmbeddedId
  private AccountHoldId id;

  @Column(name="currency", nullable=false)
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
