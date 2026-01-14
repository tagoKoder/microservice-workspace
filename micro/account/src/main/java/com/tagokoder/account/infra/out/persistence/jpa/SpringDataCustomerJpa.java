package com.tagokoder.account.infra.out.persistence.jpa;

import com.tagokoder.account.infra.out.persistence.jpa.entity.CustomerEntity;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCustomerJpa extends JpaRepository<CustomerEntity, UUID> {}
