package com.tagokoder.account.infra.out.persistence.jpa.mapper;

import com.tagokoder.account.domain.model.Customer;
import com.tagokoder.account.infra.out.persistence.jpa.entity.CustomerEntity;
import org.mapstruct.*;

import java.sql.Date;
import java.time.*;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface CustomerJpaMapper {

    // ---- Entity -> Domain ----
    @Mappings({
            @Mapping(source = "id", target = "id"),
            @Mapping(source = "fullname", target = "fullName"),
            @Mapping(source = "birthdate", target = "birthDate"),
            @Mapping(source = "status", target = "customerStatus"),
            @Mapping(source = "kycVerifiedAt", target = "kycVerifiedAt")
    })
    Customer toDomain(CustomerEntity e);

    // ---- Domain -> Entity ----
    @InheritInverseConfiguration(name = "toDomain")
    @Mappings({
            @Mapping(source = "fullName", target = "fullname"),
            @Mapping(source = "birthDate", target = "birthdate"),
            @Mapping(source = "customerStatus", target = "status"),
            @Mapping(source = "kycVerifiedAt", target = "kycVerifiedAt")
    })
    CustomerEntity fromDomain(Customer d);

    // ---- PATCH helper (actualiza solo campos no-null en entity existente) ----
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mappings({
            @Mapping(source = "fullName", target = "fullname"),
            @Mapping(source = "riskSegment", target = "riskSegment"),
            @Mapping(source = "customerStatus", target = "status")
    })
    void patchCore(@MappingTarget CustomerEntity target,
                   String fullName,
                   String riskSegment,
                   String customerStatus);

    // ---- Type conversions ----
    default LocalDate map(Date d) {
        return d == null ? null : d.toLocalDate();
    }

    default Date map(LocalDate d) {
        return d == null ? null : Date.valueOf(d);
    }

    default Instant map(LocalDateTime t) {
        return t == null ? null : t.toInstant(ZoneOffset.UTC);
    }

    default LocalDateTime map(Instant i) {
        return i == null ? null : LocalDateTime.ofInstant(i, ZoneOffset.UTC);
    }
}
