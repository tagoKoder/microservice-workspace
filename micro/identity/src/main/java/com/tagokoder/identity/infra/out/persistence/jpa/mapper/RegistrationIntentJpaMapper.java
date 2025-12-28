package com.tagokoder.identity.infra.out.persistence.jpa.mapper;

import com.tagokoder.identity.domain.model.RegistrationIntent;
import com.tagokoder.identity.infra.config.MappingConfig;
import com.tagokoder.identity.infra.out.persistence.jpa.RegistrationIntentEntity;
import org.mapstruct.Mapper;

@Mapper(config = MappingConfig.class)
public interface RegistrationIntentJpaMapper {

    RegistrationIntent toDomain(RegistrationIntentEntity entity);

    RegistrationIntentEntity fromDomain(RegistrationIntent domain);
}
