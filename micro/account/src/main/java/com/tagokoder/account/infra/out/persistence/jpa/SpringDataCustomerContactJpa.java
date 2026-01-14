package com.tagokoder.account.infra.out.persistence.jpa;

import com.tagokoder.account.infra.out.persistence.jpa.entity.CustomerContactEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataCustomerContactJpa extends JpaRepository<CustomerContactEntity, Long> {
    Optional<CustomerContactEntity> findFirstByCustomerId(UUID customerId);
}
