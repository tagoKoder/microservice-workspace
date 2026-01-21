package com.tagokoder.identity.infra.out.persistence.jpa.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "registration_intents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationIntentEntity {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "email")
  private String email;

  @Column(name = "phone")
  private String phone;

  @Column(name = "channel")
  private String channel;

  @Column(name = "state")
  private String state;

  @Column(name = "national_id", length = 32)
  private String nationalId;

  @Column(name = "national_id_issue_date")
  private LocalDate nationalIdIssueDate;

  @Column(name = "fingerprint_code", length = 64)
  private String fingerprintCode;

  @OneToMany(mappedBy = "registration", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<RegistrationKycObjectEntity> kycObjects = new ArrayList<>();

  @Column(name = "monthly_income", precision = 19, scale = 4)
  private BigDecimal monthlyIncome;

  @Column(name = "occupation_type", length = 64)
  private String occupationType;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @Column(name = "activation_ref", unique = true, length = 64)
  private String activationRef;

  @Column(name = "customer_id", length = 64)
  private String customerId;

  @Column(name = "primary_account_id", length = 64)
  private String primaryAccountId;

  @Column(name = "savings_account_id", length = 64)
  private String savingsAccountId;

  @Column(name = "bonus_journal_id", length = 64)
  private String bonusJournalId;

  @Column(name = "activated_at")
  private Instant activatedAt;
}
