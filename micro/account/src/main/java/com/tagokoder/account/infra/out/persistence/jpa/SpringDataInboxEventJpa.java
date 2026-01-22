package com.tagokoder.account.infra.out.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tagokoder.account.infra.out.persistence.jpa.entity.InboxEventEntity;

public interface SpringDataInboxEventJpa extends JpaRepository<InboxEventEntity, String> {}
