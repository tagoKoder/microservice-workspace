package com.tagokoder.ops.infra.out.persistence.jpa;

import com.tagokoder.ops.infra.out.persistence.jpa.entity.DataAccessLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataDataAccessLogJpa extends JpaRepository<DataAccessLogEntity, UUID> {}
