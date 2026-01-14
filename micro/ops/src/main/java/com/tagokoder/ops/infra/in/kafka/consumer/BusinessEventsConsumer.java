package com.tagokoder.ops.infra.in.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagokoder.ops.domain.port.in.IngestNotificationEventUseCase;
import com.tagokoder.ops.infra.in.kafka.dto.PaymentPostedEventDto;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.UUID;

public class BusinessEventsConsumer {

  private final ObjectMapper om;
  private final IngestNotificationEventUseCase ingest;

  public BusinessEventsConsumer(ObjectMapper om, IngestNotificationEventUseCase ingest) {
    this.om = om;
    this.ingest = ingest;
  }

  @KafkaListener(topics = "${ops.kafka.topics.paymentPosted}", groupId = "${ops.kafka.groupId}")
  public void onPaymentPosted(String message) throws Exception {
    PaymentPostedEventDto dto = om.readValue(message, PaymentPostedEventDto.class);
    UUID trace = dto.trace_id() != null ? UUID.fromString(dto.trace_id()) : null;

    ingest.ingest(new IngestNotificationEventUseCase.Command(
        "payment.posted",
        dto.toPayload(),
        null,
        trace
    ));
  }

  // topup.posted, onboarding.welcome: mismo patrón
}
