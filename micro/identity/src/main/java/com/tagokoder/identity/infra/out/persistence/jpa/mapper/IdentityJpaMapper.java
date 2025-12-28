package com.tagokoder.identity.infra.out.persistence.jpa.mapper;

import org.mapstruct.Mapper;

import com.tagokoder.identity.domain.model.Identity;
import com.tagokoder.identity.infra.config.MappingConfig;
import com.tagokoder.identity.infra.out.persistence.jpa.IdentityEntity;

@Mapper(config = MappingConfig.class)
public interface IdentityJpaMapper {
    Identity toDomain(IdentityEntity entity);
    IdentityEntity fromDomain(Identity identity);
}
