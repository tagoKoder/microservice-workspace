package com.tagokoder.identity.domain.model;


import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.tagokoder.identity.domain.model.kyc.FinalizedObject;
import com.tagokoder.identity.domain.model.kyc.UploadedObject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationIntent {

    public enum State {
        STARTED,
        KYC_CONFIRMED,
        ACTIVATING,
        CONSENTED,
        ACTIVATED,
        REJECTED
    }

    private  UUID id;
    private  String email;
    private  String phone;
    private  String channel;
    private  State state;

    private  String nationalId;
    private  LocalDate nationalIdIssueDate;
    private  String fingerprintCode;
    
    // Colección de objetos KYC 
    private List<FinalizedObject> kycObjects = new ArrayList<>(); 
    public void addKycObject(FinalizedObject obj) { this.kycObjects.add(obj); } 
    public void removeKycObject(FinalizedObject obj) { this.kycObjects.remove(obj); }

    private  BigDecimal monthlyIncome;
    private  String occupationType;

    private  Instant createdAt;
    private  Instant updatedAt;

    private String activationRef;
    private String customerId;
    private String primaryAccountId;   // checking
    private String savingsAccountId;   // si luego lo agregas
    private String bonusJournalId;
    private Instant activatedAt;
    private UUID identityId;
}