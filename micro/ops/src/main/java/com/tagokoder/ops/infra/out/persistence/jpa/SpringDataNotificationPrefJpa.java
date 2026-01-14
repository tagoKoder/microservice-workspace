package com.tagokoder.ops.infra.out.persistence.jpa;

import com.tagokoder.ops.infra.out.persistence.jpa.entity.NotificationPrefEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataNotificationPrefJpa extends JpaRepository<NotificationPrefEntity, UUID> {
  List<NotificationPrefEntity> findByCustomerId(UUID customerId);
  Optional<NotificationPrefEntity> findByCustomerIdAndChannel(UUID customerId, String channel);
}
