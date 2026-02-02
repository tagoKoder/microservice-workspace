package com.tagokoder.account.infra.out.persistence.jpa.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name="full_name")
    private String fullname;    
    @Column(name="birth_date", nullable=false)
    private LocalDate birthDate;
    private String tin;
    @Column(name="risk_segment")
    private String riskSegment;
    private String status;
    @Column(name="kyc_level")
    private String kycLevel;
    @Column(name="kyc_verified_at")
    private LocalDateTime kycVerifiedAt;
    @Column(name="created_at", nullable=false)
    private OffsetDateTime createdAt;
    @Column(name="updated_at", nullable=false)
    private OffsetDateTime updatedAt;
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<CustomerContactEntity> contacts;
}
