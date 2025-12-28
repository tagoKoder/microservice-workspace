package com.tagokoder.account.infra.out.persistence.jpa.entity;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id= UUID.randomUUID();
    @Column(name="full_name")
    private String fullname;
    @Column(name="birth_date")
    private Date birthdate;
    private String tin;
    @Column(name="risk_segment")
    private String riskSegment;
    private String status;
    @Column(name="kyc_level")
    private String kycLevel;
    @Column(name="kyc_verified_at")
    private LocalDateTime kycVerifiedAt;
    @OneToMany(mappedBy = "customerId", cascade = CascadeType.ALL)
    private List<CustomerContactEntity> contacts;
}
