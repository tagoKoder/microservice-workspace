package com.tagokoder.account.domain.model;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    private UUID id;
    private UUID customerId;
    private String productType; // checking/savings
    private String currency;    // USD
    private String status;      // active/frozen
    private Instant openedAt;
    private Instant updatedAt;
}
