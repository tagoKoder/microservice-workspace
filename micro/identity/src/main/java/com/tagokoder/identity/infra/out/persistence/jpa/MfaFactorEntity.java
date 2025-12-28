package com.tagokoder.identity.infra.out.persistence.jpa;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name="mfa_factors")
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
    private String type;
    private String status;
    @Column(name="created_at")
    private LocalDateTime lastVerifiedAt;
}
