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
@Table(name = "account_balances")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalanceEntity {
    @Id
    @Column(name = "account_id")
    private UUID accountId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "account_id")
    private AccountEntity account;

    @Column(name="ledger", nullable=false, precision=20, scale=6)
    private BigDecimal ledger;
    @Column(name="available", nullable=false, precision=20, scale=6)
    private BigDecimal available;
    @Column(name="hold", nullable=false, precision=20, scale=6)
    private BigDecimal hold;
}
