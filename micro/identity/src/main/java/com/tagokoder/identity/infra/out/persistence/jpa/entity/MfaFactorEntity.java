package com.tagokoder.identity.infra.out.persistence.jpa.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mfa_factors")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MfaFactorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "identity_id", nullable = false)
    private IdentityEntity identity;

    @Column(name = "type", nullable = false, length = 32)
    private String type; // "WEBAUTHN"

    @Column(name = "status", nullable = false, length = 32)
    private String status; // "ENABLED"/"DISABLED"

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt;
}
