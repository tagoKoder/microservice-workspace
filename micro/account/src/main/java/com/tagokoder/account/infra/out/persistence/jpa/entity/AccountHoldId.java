package com.tagokoder.account.infra.out.persistence.jpa.entity;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class AccountHoldId implements Serializable {
  @Column(name="account_id")
  private UUID accountId;

  @Column(name="hold_id")
  private UUID holdId;
}
