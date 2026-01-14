package com.tagokoder.ops.infra.in.grpc;

import bank.ops.v1.*;

import com.google.protobuf.Timestamp;

import com.tagokoder.ops.domain.port.in.GetAuditByTraceUseCase;
import com.tagokoder.ops.domain.port.out.DataAccessLogPort;

import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class AuditGrpcService extends AuditServiceGrpc.AuditServiceImplBase {

  private final GetAuditByTraceUseCase query;
  private final DataAccessLogPort accessLog;

  public AuditGrpcService(GetAuditByTraceUseCase query, DataAccessLogPort accessLog) {
    this.query = query;
    this.accessLog = accessLog;
  }

  @Override
  public void getAuditEventsByTrace(GetAuditEventsByTraceRequest request,
                                   StreamObserver<GetAuditEventsByTraceResponse> responseObserver) {

    String subject = request.hasXSubject() ? request.getXSubject().getValue() : "internal";
    int limit = request.hasLimit() ? request.getLimit().getValue() : 200;

    UUID traceId = UUID.fromString(request.getTraceId());

    accessLog.log(
        subject,
        "READ",
        "audit_events",
        null,
        "trace_lookup",
        Instant.now()
    );

    var items = query.get(traceId, limit).stream()
        .map(e -> AuditEventItem.newBuilder()
            .setAction(e.action() != null ? e.action() : "")
            .setOccurredAt(toTimestamp(e.occurredAt()))
            .setResourceId(e.resourceId() != null ? e.resourceId() : "")
            .build()
        )
        .toList();

    var response = GetAuditEventsByTraceResponse.newBuilder()
        .addAllEvents(items)
        .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  private Timestamp toTimestamp(Instant instant) {
    if (instant == null) {
      return Timestamp.newBuilder().setSeconds(0).setNanos(0).build();
    }
    return Timestamp.newBuilder()
        .setSeconds(instant.getEpochSecond())
        .setNanos(instant.getNano())
        .build();
  }
}
