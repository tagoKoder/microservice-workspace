package com.tagokoder.ops.infra.in.rest.controller;

import com.tagokoder.ops.domain.port.in.GetAuditByTraceUseCase;
import com.tagokoder.ops.domain.port.out.DataAccessLogPort;
import com.tagokoder.ops.infra.in.rest.dto.AuditEventsResponseDto;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/audit")
public class AuditQueryController {

  private final GetAuditByTraceUseCase query;
  private final DataAccessLogPort accessLog;

  public AuditQueryController(GetAuditByTraceUseCase query, DataAccessLogPort accessLog) {
    this.query = query;
    this.accessLog = accessLog;
  }

  @GetMapping("/events")
  public AuditEventsResponseDto getByTrace(@RequestParam("trace_id") UUID traceId,
                                          @RequestParam(value = "limit", required = false) Integer limit,
                                          @RequestHeader(value = "X-Subject", required = false) String subject) {

    accessLog.log(subject != null ? subject : "internal",
        "READ", "audit_events", null, "trace_lookup", Instant.now());

    var events = query.get(traceId, limit != null ? limit : 200).stream()
        .map(e -> new AuditEventsResponseDto.Item(e.action(), e.occurredAt().toString(), e.resourceId()))
        .toList();

    return new AuditEventsResponseDto(events);
  }
}
