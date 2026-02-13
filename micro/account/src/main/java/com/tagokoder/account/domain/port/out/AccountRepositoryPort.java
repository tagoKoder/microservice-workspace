package com.tagokoder.account.domain.port.out;

import com.tagokoder.account.domain.model.Account;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepositoryPort {
    Account save(Account account);
    Optional<Account> findById(UUID id);
    List<Account> findByCustomerId(UUID customerId);
    boolean existsCustomer(UUID customerId);
    List<Account> findByIds(List<UUID> accountIds);
}
