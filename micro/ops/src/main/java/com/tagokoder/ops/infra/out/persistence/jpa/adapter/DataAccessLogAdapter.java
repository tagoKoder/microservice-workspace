package com.tagokoder.ops.infra.out.persistence.jpa.adapter;

import com.tagokoder.ops.domain.port.out.DataAccessLogPort;
import com.tagokoder.ops.infra.out.persistence.jpa.entity.DataAccessLogEntity;
import com.tagokoder.ops.infra.out.persistence.jpa.SpringDataDataAccessLogJpa;

import java.time.Instant;
import java.util.UUID;

public class DataAccessLogAdapter implements DataAccessLogPort {

  private final SpringDataDataAccessLogJpa jpa;

  public DataAccessLogAdapter(SpringDataDataAccessLogJpa jpa) {
    this.jpa = jpa;
  }

  @Override
  public void log(String subject, String operation, String table, UUID recordId, String purpose, Instant occurredAt) {
    DataAccessLogEntity e = new DataAccessLogEntity();
    e.setId(UUID.randomUUID());
    e.setSubject(subject);
    e.setOperation(operation);
    e.setTableName(table);
    e.setRecordId(recordId);
    e.setPurpose(purpose);
    e.setOccurredAt(occurredAt);
    jpa.save(e);
  }
}
