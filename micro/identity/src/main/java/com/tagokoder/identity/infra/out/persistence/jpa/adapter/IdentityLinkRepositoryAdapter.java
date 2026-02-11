package com.tagokoder.identity.infra.out.persistence.jpa.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.tagokoder.identity.domain.port.out.IdentityLinkRepositoryPort;
import com.tagokoder.identity.infra.out.persistence.jpa.SpringDataIdentityLinkJpa;
import com.tagokoder.identity.infra.out.persistence.jpa.entity.IdentityLinkEntity;

@Component
public class IdentityLinkRepositoryAdapter implements IdentityLinkRepositoryPort {

  private final SpringDataIdentityLinkJpa jpa;

  public IdentityLinkRepositoryAdapter(SpringDataIdentityLinkJpa jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<String> findCustomerIdByIdentityId(UUID identityId) {
    return jpa.findByIdentityId(identityId).map(IdentityLinkEntity::getCustomerId);
  }

  @Override
  public void upsert(UUID identityId, String customerId) {
    if (identityId == null || customerId == null || customerId.isBlank()) return;

    // Save() sobre PK identity_id = upsert natural
    jpa.save(new IdentityLinkEntity(identityId, customerId));
  }
}
