package com.tagokoder.identity.domain.port.out;

import java.util.Optional;
import java.util.UUID;

public interface IdentityLinkRepositoryPort {
  Optional<String> findCustomerIdByIdentityId(UUID identityId);
  void upsert(UUID identityId, String customerId);
}