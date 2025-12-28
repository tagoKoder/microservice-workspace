package com.tagokoder.account.infra.out.persistence.jpa;

import com.tagokoder.account.infra.out.persistence.jpa.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataAccountJpa extends JpaRepository<AccountEntity, UUID> {
    List<AccountEntity> findByCustomerId(UUID customerId);
}
