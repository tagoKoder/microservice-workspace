package com.tagokoder.account.infra.out.persistence.jpa.entity;
import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "account_balances")
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

    private BigDecimal ledger;
    private BigDecimal available;
    private BigDecimal hold;
}
