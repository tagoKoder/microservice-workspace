package com.tagokoder.identity.infra.out.persistence.jpa.mapper;

import java.util.UUID;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import com.tagokoder.identity.infra.out.persistence.jpa.entity.IdentityEntity;

@Component
public class IdentityEntityRefResolver {
  private final EntityManager em;
  public IdentityEntityRefResolver(EntityManager em) { this.em = em; }

  public IdentityEntity ref(UUID id) {
    return id == null ? null : em.getReference(IdentityEntity.class, id);
  }
}