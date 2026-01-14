package com.tagokoder.ops.infra.out.persistence.jpa.adapter;

import com.tagokoder.ops.domain.model.Channel;
import com.tagokoder.ops.domain.model.NotificationPref;
import com.tagokoder.ops.domain.port.out.NotificationPrefRepositoryPort;
import com.tagokoder.ops.infra.out.persistence.jpa.entity.NotificationPrefEntity;
import com.tagokoder.ops.infra.out.persistence.jpa.SpringDataNotificationPrefJpa;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class NotificationPrefRepositoryAdapter implements NotificationPrefRepositoryPort {

  private final SpringDataNotificationPrefJpa jpa;

  public NotificationPrefRepositoryAdapter(SpringDataNotificationPrefJpa jpa) {
    this.jpa = jpa;
  }

  @Override
  public List<NotificationPref> findByCustomer(UUID customerId) {
    return jpa.findByCustomerId(customerId).stream()
        .map(e -> new NotificationPref(e.getId(), e.getCustomerId(), Channel.fromWire(e.getChannel()), e.isOptIn(), e.getUpdatedAt()))
        .toList();
  }

  @Override
  public Optional<NotificationPref> findByCustomerAndChannel(UUID customerId, Channel channel) {
    return jpa.findByCustomerIdAndChannel(customerId, channel.toWire())
        .map(e -> new NotificationPref(e.getId(), e.getCustomerId(), Channel.fromWire(e.getChannel()), e.isOptIn(), e.getUpdatedAt()));
  }

  @Override
  public void upsert(UUID customerId, Channel channel, boolean optIn) {
    NotificationPrefEntity ent = jpa.findByCustomerIdAndChannel(customerId, channel.toWire())
        .orElseGet(() -> {
          NotificationPrefEntity x = new NotificationPrefEntity();
          x.setId(UUID.randomUUID());
          x.setCustomerId(customerId);
          x.setChannel(channel.toWire());
          x.setOptIn(optIn);
          x.setUpdatedAt(Instant.now());
          return x;
        });

    ent.setOptIn(optIn);
    ent.setUpdatedAt(Instant.now());
    jpa.save(ent);
  }
}
