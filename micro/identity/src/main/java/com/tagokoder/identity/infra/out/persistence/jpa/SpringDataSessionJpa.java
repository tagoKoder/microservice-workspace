package com.tagokoder.identity.infra.out.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tagokoder.identity.infra.out.persistence.jpa.entity.SessionEntity;

public interface SpringDataSessionJpa extends JpaRepository<SessionEntity, java.util.UUID>  {
    
}
