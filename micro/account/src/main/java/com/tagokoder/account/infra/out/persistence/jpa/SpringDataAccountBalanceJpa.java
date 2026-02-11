package com.tagokoder.account.infra.out.persistence.jpa;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tagokoder.account.infra.out.persistence.jpa.entity.AccountBalanceEntity;

public interface SpringDataAccountBalanceJpa extends JpaRepository<AccountBalanceEntity, UUID> {
    Optional<AccountBalanceEntity> findByAccountId(UUID accountId);
  @Modifying
  @Query(value = """
    update account_balances
    set hold = hold + :amount,
        available = available - :amount
    where account_id = :accountId
      and available >= :amount
  """, nativeQuery = true)
  int reserveHoldAtomic(@Param("accountId") UUID accountId, @Param("amount") BigDecimal amount);

  @Modifying
  @Query(value = """
    update account_balances
    set hold = hold - :amount,
        available = available + :amount
    where account_id = :accountId
      and hold >= :amount
  """, nativeQuery = true)
  int releaseHoldAtomic(@Param("accountId") UUID accountId, @Param("amount") BigDecimal amount);

  @Modifying
  @Query(value = """
    update account_balances
    set ledger = ledger + :dLedger,
        available = available + :dAvailable,
        hold = hold + :dHold
    where account_id = :accountId
  """, nativeQuery = true)
  int applyDeltas(@Param("accountId") UUID accountId,
                  @Param("dLedger") BigDecimal dLedger,
                  @Param("dAvailable") BigDecimal dAvailable,
                  @Param("dHold") BigDecimal dHold);

    @Modifying
    @Query(value = """
        INSERT INTO account_balances(account_id, ledger, available, hold)
        VALUES (:accountId, :ledger, :available, :hold)
        ON CONFLICT (account_id) DO NOTHING
        """, nativeQuery = true)
    int initIfAbsent(
            @Param("accountId") UUID accountId,
            @Param("ledger") BigDecimal ledger,
            @Param("available") BigDecimal available,
            @Param("hold") BigDecimal hold
    );

}
