package com.tagokoder.account.infra.out.persistence.jpa;

import com.tagokoder.account.infra.out.persistence.jpa.entity.AccountBalanceEntity;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataAccountBalanceJpa extends JpaRepository<AccountBalanceEntity, UUID> {
    Optional<AccountBalanceEntity> findByAccountId(UUID accountId);
        @Modifying
    @Query("update account_balances b set b.hold = b.hold + :amount where b.accountId = :accountId")
    int addHold(@Param("accountId") UUID accountId, @Param("amount") java.math.BigDecimal amount);

    @Modifying
    @Query("update account_balances b set b.hold = case when (b.hold - :amount) < 0 then 0 else (b.hold - :amount) end where b.accountId = :accountId")
    int subHold(@Param("accountId") UUID accountId, @Param("amount") java.math.BigDecimal amount);

    @Modifying
    @Query("UPDATE account_balances b SET b.hold = b.hold + :amt, b.available = b.available - :amt WHERE b.accountId = :id AND b.available >= :amt")
    int reserve(UUID id, BigDecimal amt);

    @Modifying
    @Query("UPDATE account_balances b SET b.hold = b.hold - :amt, b.available = b.available + :amt WHERE b.accountId = :id AND b.hold >= :amt")
    int release(UUID id, BigDecimal amt);

}
