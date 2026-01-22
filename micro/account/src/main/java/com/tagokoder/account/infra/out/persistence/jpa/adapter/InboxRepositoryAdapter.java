package com.tagokoder.account.infra.out.persistence.jpa.adapter;

import java.time.OffsetDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tagokoder.account.domain.port.out.InboxRepositoryPort;
import com.tagokoder.account.infra.out.persistence.jpa.SpringDataInboxEventJpa;
import com.tagokoder.account.infra.out.persistence.jpa.entity.InboxEventEntity;

@Component
public class InboxRepositoryAdapter implements InboxRepositoryPort {

  private final SpringDataInboxEventJpa jpa;

  public InboxRepositoryAdapter(SpringDataInboxEventJpa jpa) {
    this.jpa = jpa;
  }

  @Override
  @Transactional
  public boolean tryBegin(String eventId, String eventType) {
    if (eventId == null || eventId.isBlank()) return true; // no dedupe posible, procesa igual

    try {
      InboxEventEntity e = new InboxEventEntity();
      e.setEventId(eventId);
      e.setEventType(eventType);
      e.setReceivedAt(OffsetDateTime.now());
      e.setStatus("received");
      jpa.save(e);
      return true;
    } catch (DataIntegrityViolationException dup) {
      // ya existe: si está processed -> no reprocesar
      var existing = jpa.findById(eventId).orElse(null);
      return existing != null && !"processed".equalsIgnoreCase(existing.getStatus());
    }
  }

  @Override
  @Transactional
  public void markProcessed(String eventId) {
    if (eventId == null || eventId.isBlank()) return;
    var e = jpa.findById(eventId).orElse(null);
    if (e == null) return;
    e.setStatus("processed");
    e.setProcessedAt(OffsetDateTime.now());
    e.setError(null);
    jpa.save(e);
  }

  @Override
  @Transactional
  public void markFailedSafe(String eventIdOrNull, String eventType, String error) {
    if (eventIdOrNull == null || eventIdOrNull.isBlank()) return;
    var e = jpa.findById(eventIdOrNull).orElseGet(() -> {
      InboxEventEntity x = new InboxEventEntity();
      x.setEventId(eventIdOrNull);
      x.setEventType(eventType);
      x.setReceivedAt(OffsetDateTime.now());
      x.setStatus("received");
      return x;
    });
    e.setStatus("failed");
    e.setError(error);
    jpa.save(e);
  }
}
