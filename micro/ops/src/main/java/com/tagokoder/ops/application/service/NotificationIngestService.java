package com.tagokoder.ops.application.service;

import com.tagokoder.ops.domain.model.Channel;
import com.tagokoder.ops.domain.model.NotificationEvent;
import com.tagokoder.ops.domain.port.in.IngestNotificationEventUseCase;
import com.tagokoder.ops.domain.port.out.NotificationEventRepositoryPort;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class NotificationIngestService implements IngestNotificationEventUseCase {

  private final NotificationEventRepositoryPort repo;

  public NotificationIngestService(NotificationEventRepositoryPort repo) {
    this.repo = repo;
  }

  @Override
  public Result ingest(Command c) {
    Channel ch = null;
    if (c.channelOverride() != null && !c.channelOverride().isBlank() && !"null".equalsIgnoreCase(c.channelOverride())) {
      ch = Channel.fromWire(c.channelOverride());
    }

    NotificationEvent e = new NotificationEvent(
        null,
        c.topic(),
        c.payload() != null ? c.payload() : Map.of(),
        ch,
        "queued",
        0,
        null,
        null,
        c.traceId(),
        Instant.now()
    );

    UUID id = repo.insertQueued(e);
    return new Result(true, id);
  }
}
