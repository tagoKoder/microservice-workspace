package com.santiago_tumbaco.identity.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.santiago_tumbaco.identity.domain.model.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount,Long> {
    Optional<UserAccount> findByIdpIssuerAndIdpSub(String iss, String sub);
}
