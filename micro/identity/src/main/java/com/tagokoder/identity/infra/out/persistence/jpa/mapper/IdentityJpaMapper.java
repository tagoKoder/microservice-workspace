package com.tagokoder.identity.infra.out.persistence.jpa.mapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import com.tagokoder.identity.domain.model.Identity;
import com.tagokoder.identity.infra.config.MappingConfig;
import com.tagokoder.identity.infra.out.persistence.jpa.entity.IdentityEntity;

@Mapper(config = MappingConfig.class)
public interface IdentityJpaMapper {
    IdentityJpaMapper INSTANCE = Mappers.getMapper(IdentityJpaMapper.class);

    default Instant map(OffsetDateTime odt) {
        return odt == null ? null : odt.toInstant();
    }

    // Instant <-> LocalDateTime
    default Instant map(java.time.LocalDateTime ldt) {
        return ldt == null ? null : ldt.toInstant(ZoneOffset.UTC);
    }

    default java.time.LocalDateTime map(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    // Mapeos principales
    Identity toDomain(IdentityEntity entity);
    
    @Mapping(target = "sessions", ignore = true)
    IdentityEntity fromDomain(Identity identity);
}