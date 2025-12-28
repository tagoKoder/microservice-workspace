package com.tagokoder.identity.infra.out.persistence.jpa;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Data
@Table(name = "registration_intents")
public class RegistrationIntentEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "channel", nullable = false)
    private String channel; // 'web'

    @Column(name = "state", nullable = false)
    private String state; // 'started','contact_verified','consented','activated','rejected'

    // KYC específicos
    @Column(name = "national_id", nullable = false, length = 20)
    private String nationalId;

    @Column(name = "national_id_issue_date", nullable = false)
    private LocalDate nationalIdIssueDate;

    @Column(name = "fingerprint_code", nullable = false, length = 64)
    private String fingerprintCode;

    @Column(name = "id_document_front_url", nullable = false)
    private String idDocumentFrontUrl;

    @Column(name = "selfie_url", nullable = false)
    private String selfieUrl;

    @Column(name = "monthly_income", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyIncome;

    @Column(name = "occupation_type", nullable = false)
    private String occupationType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
