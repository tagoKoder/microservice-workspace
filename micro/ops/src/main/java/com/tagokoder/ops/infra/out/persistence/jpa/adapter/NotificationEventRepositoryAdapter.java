package com.tagokoder.ops.infra.out.persistence.jpa.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagokoder.ops.domain.model.Channel;
import com.tagokoder.ops.domain.model.NotificationEvent;
import com.tagokoder.ops.domain.port.out.NotificationEventRepositoryPort;
import com.tagokoder.ops.infra.out.persistence.jpa.entity.NotificationEventEntity;
import com.tagokoder.ops.infra.out.persistence.jpa.SpringDataNotificationEventJpa;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

public class NotificationEventRepositoryAdapter implements NotificationEventRepositoryPort {

  private final SpringDataNotificationEventJpa jpa;
  private final ObjectMapper om;

  public NotificationEventRepositoryAdapter(SpringDataNotificationEventJpa jpa, ObjectMapper om) {
    this.jpa = jpa;
    this.om = om;
  }

  @Override
  public UUID insertQueued(NotificationEvent e) {
    NotificationEventEntity ent = new NotificationEventEntity();
    UUID id = UUID.randomUUID();
    ent.setId(id);
    ent.setTopic(e.topic());
    try {
      ent.setPayloadJson(om.writeValueAsString(e.payload()));
    } catch (Exception ex) {
      throw new IllegalArgumentException("invalid payload");
    }
    ent.setChannelOverride(e.channelOverride() != null ? e.channelOverride().toWire() : null);
    ent.setStatus("queued");
    ent.setRetryCount(0);
    ent.setNextRetryAt(null);
    ent.setLastError(null);
    ent.setTraceId(e.traceId());
    ent.setCreatedAt(Instant.now());
    ent.setUpdatedAt(Instant.now());
    jpa.save(ent);
    return id;
  }

  @Override
  @Transactional
  public List<NotificationEvent> fetchNextDueForUpdate(int limit, Instant now) {
    if (limit <= 0) limit = 50;
    List<NotificationEventEntity> rows = jpa.fetchNextDueForUpdate(now, limit);

    List<NotificationEvent> out = new ArrayList<>(rows.size());
    for (NotificationEventEntity r : rows) {
      Map<String,Object> payload;
      try {
        payload = om.readValue(r.getPayloadJson(), Map.class);
      } catch (Exception ex) {
        payload = Map.of();
      }

      Channel override = null;
      if (r.getChannelOverride() != null) override = Channel.fromWire(r.getChannelOverride());

      out.add(new NotificationEvent(
          r.getId(),
          r.getTopic(),
          payload,
          override,
          r.getStatus(),
          r.getRetryCount(),
          r.getNextRetryAt(),
          r.getLastError(),
          r.getTraceId(),
          r.getCreatedAt()
      ));
    }
    return out;
  }

  @Override
  public void markSent(UUID id) {
    NotificationEventEntity e = jpa.findById(id).orElseThrow();
    e.setStatus("sent");
    e.setUpdatedAt(Instant.now());
    jpa.save(e);
  }

  @Override
  public void markFailed(UUID id, int retryCount, Instant nextRetryAt, String lastError, boolean terminal) {
    NotificationEventEntity e = jpa.findById(id).orElseThrow();
    e.setRetryCount(retryCount);
    e.setLastError(lastError);
    e.setNextRetryAt(nextRetryAt);
    e.setStatus(terminal ? "failed" : "queued");
    e.setUpdatedAt(Instant.now());
    jpa.save(e);
  }
}
