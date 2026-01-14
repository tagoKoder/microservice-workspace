package com.tagokoder.identity.infra.out.persistence.jpa.mapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.tagokoder.identity.domain.model.Session;
import com.tagokoder.identity.infra.config.MappingConfig;
import com.tagokoder.identity.infra.out.persistence.jpa.entity.SessionEntity;

@Mapper(config = MappingConfig.class)
public interface SessionJpaMapper {

  @Mapping(target = "identityId", source = "identity.id")
  Session toDomain(SessionEntity entity);

  // identity se setea en el Adapter vía getReferenceById (no aquí)
  // Talvez modificar para que funcione con el resolver?
  @Mapping(target = "identity", ignore = true)
  SessionEntity fromDomain(Session session);

  // Instant <-> LocalDateTime (UTC)
  default Instant map(LocalDateTime ldt) {
    return ldt == null ? null : ldt.toInstant(ZoneOffset.UTC);
  }

  default LocalDateTime map(Instant instant) {
    return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
  }
}