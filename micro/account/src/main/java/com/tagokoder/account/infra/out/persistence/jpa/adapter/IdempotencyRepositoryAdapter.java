package com.tagokoder.account.infra.out.persistence.jpa.adapter;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tagokoder.account.domain.port.out.IdempotencyRepositoryPort;
import com.tagokoder.account.infra.out.persistence.jpa.SpringDataIdempotencyJpa;
import com.tagokoder.account.infra.out.persistence.jpa.entity.IdempotencyRecordEntity;

@Component
public class IdempotencyRepositoryAdapter implements IdempotencyRepositoryPort {

  private final SpringDataIdempotencyJpa jpa;

  public IdempotencyRepositoryAdapter(SpringDataIdempotencyJpa jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<Record> find(String key) {
    if (key == null || key.isBlank()) return Optional.empty();
    return jpa.findByKey(key).map(e -> new Record(e.getKey(), e.getOperation(), e.getStatusCode(), e.getResponseJson()));
  }

  @Override
  @Transactional
  public void save(String key, String operation, int statusCode, String responseJson) {
    IdempotencyRecordEntity e = new IdempotencyRecordEntity();
    e.setKey(key);
    e.setOperation(operation);
    e.setStatusCode(statusCode);
    e.setResponseJson(responseJson);
    e.setCreatedAt(OffsetDateTime.now());
    jpa.save(e);
  }
}
