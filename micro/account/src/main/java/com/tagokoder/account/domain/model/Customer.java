package com.tagokoder.account.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Customer {
    private UUID id;
    private String fullName;
    private LocalDate birthDate;
    private String tin;
    private String riskSegment;
    private String customerStatus;
    private String kycLevel;
    private Instant kycVerifiedAt;
}
