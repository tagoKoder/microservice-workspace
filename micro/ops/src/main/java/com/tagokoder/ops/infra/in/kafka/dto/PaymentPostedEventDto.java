package com.tagokoder.ops.infra.in.kafka.dto;

import java.util.Map;

public record PaymentPostedEventDto(
    String payment_id,
    String customer_id,
    String account_id,
    String amount,
    String currency,
    String trace_id,
    String occurred_at
) {
  public Map<String,Object> toPayload() {
    return Map.of(
        "payment_id", payment_id,
        "customer_id", customer_id,
        "account_id", account_id,
        "amount", amount,
        "currency", currency
    );
  }
}
