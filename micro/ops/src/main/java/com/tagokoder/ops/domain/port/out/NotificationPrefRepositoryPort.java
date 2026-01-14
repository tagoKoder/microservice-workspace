package com.tagokoder.ops.domain.port.out;

import com.tagokoder.ops.domain.model.NotificationPref;
import com.tagokoder.ops.domain.model.Channel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationPrefRepositoryPort {
  List<NotificationPref> findByCustomer(UUID customerId);
  Optional<NotificationPref> findByCustomerAndChannel(UUID customerId, Channel channel);
  void upsert(UUID customerId, Channel channel, boolean optIn);
}
