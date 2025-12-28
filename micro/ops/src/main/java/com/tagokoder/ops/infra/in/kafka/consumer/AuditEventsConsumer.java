package com.tagokoder.ops.infra.in.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagokoder.ops.domain.port.in.IngestAuditEventUseCase;
import com.tagokoder.ops.infra.in.kafka.dto.AuditEventDto;
import java.time.Instant;
import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;

public class AuditEventsConsumer {

  private final ObjectMapper om;
  private final IngestAuditEventUseCase ingest;

  public AuditEventsConsumer(ObjectMapper om, IngestAuditEventUseCase ingest) {
    this.om = om;
    this.ingest = ingest;
  }

  @KafkaListener(topics = "${ops.kafka.topics.auditEvents}", groupId = "${ops.kafka.groupId}")
  public void onAuditEvent(String message) throws Exception {
    AuditEventDto dto = om.readValue(message, AuditEventDto.class);

    ingest.ingest(new IngestAuditEventUseCase.Command(
        dto.actor_type(),
        dto.actor_id() != null && !"null".equals(dto.actor_id()) ? UUID.fromString(dto.actor_id()) : null,
        dto.action(),
        dto.resource(),
        dto.resource_id(),
        dto.ip(),
        dto.user_agent(),
        UUID.fromString(dto.trace_id()),
        Instant.parse(dto.occurred_at())
    ));
  }
}
