package com.tagokoder.identity.domain.model;


import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationIntent {

    public enum State {
        STARTED,
        CONTACT_VERIFIED,
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
    private  String idDocumentFrontUrl;
    private  String selfieUrl;
    private  BigDecimal monthlyIncome;
    private  String occupationType;

    private  Instant createdAt;
    private  Instant updatedAt;
}