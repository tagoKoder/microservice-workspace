package com.tagokoder.account.infra.out.persistence.jpa.mapper;
import com.tagokoder.account.domain.model.Account;
import com.tagokoder.account.infra.config.MappingConfig;
import com.tagokoder.account.infra.out.persistence.jpa.entity.AccountEntity;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(config = MappingConfig.class)
public interface AccountJpaMapper {
    AccountJpaMapper INSTANCE = Mappers.getMapper(AccountJpaMapper.class);
    default OffsetDateTime map(Instant instant) { return instant == null ? null : instant.atOffset(ZoneOffset.UTC); } 
    default Instant map(OffsetDateTime odt) { return odt == null ? null : odt.toInstant(); }
    AccountEntity fromDomain(Account account);
    Account toDomain(AccountEntity entity);
}