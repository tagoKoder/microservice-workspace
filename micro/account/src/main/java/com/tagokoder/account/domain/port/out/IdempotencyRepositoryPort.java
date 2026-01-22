package com.tagokoder.account.domain.port.out;

import java.util.Optional;

public interface IdempotencyRepositoryPort {
  Optional<Record> find(String key);
  void save(String key, String operation, int statusCode, String responseJson);

  record Record(String key, String operation, int statusCode, String responseJson) {}
}
