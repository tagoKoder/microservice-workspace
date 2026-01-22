package com.tagokoder.account.application.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagokoder.account.domain.port.out.IdempotencyRepositoryPort;

@Service
public class IdempotencyService {

  private final IdempotencyRepositoryPort repo;
  private final ObjectMapper om = new ObjectMapper();

  public IdempotencyService(IdempotencyRepositoryPort repo) {
    this.repo = repo;
  }

  public <T> Optional<T> tryGet(String key, String operation, Class<T> clazz) {
    if (key == null || key.isBlank()) return Optional.empty();
    return repo.find(key)
      .filter(r -> operation.equals(r.operation()))
      .flatMap(r -> {
        try { return Optional.of(om.readValue(r.responseJson(), clazz)); }
        catch (Exception e) { return Optional.empty(); }
      });
  }

  public void save(String key, String operation, int statusCode, Object response) {
    if (key == null || key.isBlank()) return;
    try {
      String json = om.writeValueAsString(response);
      repo.save(key, operation, statusCode, json);
    } catch (Exception ignored) {}
  }
}
