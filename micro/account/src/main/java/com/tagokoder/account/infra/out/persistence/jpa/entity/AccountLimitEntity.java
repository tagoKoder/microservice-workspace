package com.tagokoder.account.infra.out.persistence.jpa.entity;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "account_limits")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountLimitEntity {
    @Id
    @Column(name = "account_id")
    private UUID accountId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "account_id")
    private AccountEntity account;

    @Column(name = "daily_out", nullable = false, precision = 20, scale = 6)
    private BigDecimal dailyOut;

    @Column(name = "daily_in", nullable = false, precision = 20, scale = 6)
    private BigDecimal dailyIn;
}
