package com.tagokoder.ops.infra.in.rest.dto;

import java.util.List;

public record AuditEventsResponseDto(List<Item> events) {
  public record Item(String action, String occurred_at, String resource_id) {}
}
