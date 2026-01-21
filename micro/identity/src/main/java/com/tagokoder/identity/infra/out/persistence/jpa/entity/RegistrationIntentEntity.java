package com.tagokoder.identity.infra.out.persistence.jpa.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.checkerframework.checker.units.qual.C;

import jakarta.persistence.Column;
import lombok.Data;

@Data
public class RegistrationIntentEntity {

    private UUID id;
    private String email;
    private String phone;
    private String channel; // 'web'
    private String state;   // 'started','contact_verified','consented','activated','rejected'

    // KYC específicos
    @Column(name = "national_id", length = 32)
    private String nationalId;
    @Column(name = "national_id_issue_date")
    private LocalDate nationalIdIssueDate;
    @Column(name = "fingerprint_code", length = 64)
    private String fingerprintCode;

    // Colección de objetos KYC
    private List<RegistrationKycObjectEntity> kycObjects = new ArrayList<>();

    public void addKycObject(RegistrationKycObjectEntity obj) {
        this.kycObjects.add(obj);
    }

    public void removeKycObject(RegistrationKycObjectEntity obj) {
        this.kycObjects.remove(obj);
    }
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
    // Puedes reutilizar "state", pero si prefieres separar: 
    @Column(name = "activation_state", length = 32) 
    private String activationState; 
    @Column(name = "customer_id", length = 64) 
    private String customerId; 
    @Column(name = "checking_account_id", length = 64) 
    private String checkingAccountId; 
    @Column(name = "savings_account_id", length = 64) 
    private String savingsAccountId; 
    // opcional 
    @Column(name = "bonus_journal_id", length = 64) 
    private String bonusJournalId; 
    // opcional 
    @Column(name = "activated_at") 
    private Instant activatedAt;
}
