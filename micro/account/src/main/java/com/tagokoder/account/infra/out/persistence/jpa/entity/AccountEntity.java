package com.tagokoder.account.infra.out.persistence.jpa.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;
    
    @Generated(event = EventType.INSERT)
    @Column(name="account_number", nullable=false, unique=true, updatable=false, insertable=false)
    private Long accountNumber;

    @Column(name = "product_type")
    private String productType;
    @Column(name ="currency", nullable = false)
    private String currency;
    private String status;

    @Column(name = "opened_at")
    private OffsetDateTime openedAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private AccountBalanceEntity balance;

    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private AccountLimitEntity limit;

    @PrePersist
    void prePersist() {
    if (openedAt == null) openedAt = OffsetDateTime.now();
    if (updatedAt == null) updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
    updatedAt = OffsetDateTime.now();
    }
}
