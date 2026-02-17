package com.tagokoder.account.infra.out.persistence.jpa.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.tagokoder.account.domain.model.Account;
import com.tagokoder.account.domain.port.out.AccountRepositoryPort;
import com.tagokoder.account.infra.out.persistence.jpa.SpringDataAccountJpa;
import com.tagokoder.account.infra.out.persistence.jpa.SpringDataCustomerJpa;
import com.tagokoder.account.infra.out.persistence.jpa.mapper.AccountJpaMapper;

@Component
public class AccountRepositoryAdapter implements AccountRepositoryPort {

    private final SpringDataAccountJpa accountJpa;
    private final SpringDataCustomerJpa customerJpa;
    private final AccountJpaMapper mapper;

    public AccountRepositoryAdapter(SpringDataAccountJpa accountJpa,
                                    SpringDataCustomerJpa customerJpa,
                                    AccountJpaMapper mapper) {
        this.accountJpa = accountJpa;
        this.customerJpa = customerJpa;
        this.mapper = mapper;
    }

    @Override
    public Account save(Account account) {
        var entity = mapper.fromDomain(account);
        var saved = accountJpa.saveAndFlush(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Account> findById(UUID accountId) {
        return accountJpa.findById(accountId).map(mapper::toDomain);
    }

    @Override
    public List<Account> findByCustomerId(UUID customerId) {
        return accountJpa.findByCustomerId(customerId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsCustomer(UUID customerId) {
        return customerJpa.existsById(customerId);
    }

    @Override
    public List<Account> findByIds(List<UUID> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) return List.of();
        return accountJpa.findByIdIn(accountIds).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Account> findByAccountNumber(long accountNumber) {
    return accountJpa.findByAccountNumber(accountNumber).map(mapper::toDomain);
    }
}
