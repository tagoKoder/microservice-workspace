package com.tagokoder.account.infra.out.persistence.jpa.adapter;

import com.tagokoder.account.domain.model.Customer;
import com.tagokoder.account.domain.port.out.CustomerRepositoryPort;
import com.tagokoder.account.infra.out.persistence.jpa.SpringDataCustomerContactJpa;
import com.tagokoder.account.infra.out.persistence.jpa.SpringDataCustomerJpa;
import com.tagokoder.account.infra.out.persistence.jpa.SpringDataPreferenceJpa;
import com.tagokoder.account.infra.out.persistence.jpa.entity.CustomerContactEntity;
import com.tagokoder.account.infra.out.persistence.jpa.entity.PreferenceEntity;
import com.tagokoder.account.infra.out.persistence.jpa.mapper.CustomerJpaMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class CustomerRepositoryAdapter implements CustomerRepositoryPort {

    private final SpringDataCustomerJpa customerJpa;
    private final SpringDataCustomerContactJpa contactJpa;
    private final SpringDataPreferenceJpa preferenceJpa;
    private final CustomerJpaMapper mapper;

    public CustomerRepositoryAdapter(SpringDataCustomerJpa customerJpa,
                                     SpringDataCustomerContactJpa contactJpa,
                                     SpringDataPreferenceJpa preferenceJpa,
                                     CustomerJpaMapper mapper) {
        this.customerJpa = customerJpa;
        this.contactJpa = contactJpa;
        this.preferenceJpa = preferenceJpa;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Customer save(Customer customer) {
        var entity = mapper.fromDomain(customer);
        var saved = customerJpa.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Customer> findById(UUID id) {
        return customerJpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void updateCoreFields(UUID id, String fullNameOrNull, String riskSegmentOrNull, String statusOrNull) {
        var e = customerJpa.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));

        if (fullNameOrNull != null) e.setFullname(fullNameOrNull);
        if (riskSegmentOrNull != null) e.setRiskSegment(riskSegmentOrNull);
        if (statusOrNull != null) e.setStatus(statusOrNull);

        customerJpa.save(e);
    }

    @Override
    @Transactional
    public void upsertContact(UUID customerId, String emailOrNull, String phoneOrNull) {
        var c = contactJpa.findFirstByCustomerId(customerId).orElseGet(() -> {
            var x = new CustomerContactEntity();
            x.setCustomerId(customerId);
            x.setEmailVerified(false);
            return x;
        });

        if (emailOrNull != null) c.setEmail(emailOrNull);
        if (phoneOrNull != null) c.setPhone(phoneOrNull);

        contactJpa.save(c);
    }

    @Override
    @Transactional
    public void upsertPreference(UUID customerId, String channelOrNull, Boolean optInOrNull) {
        var p = preferenceJpa.findFirstByCustomerId(customerId).orElseGet(() -> {
            var x = new PreferenceEntity();
            x.setCustomerId(customerId);
            x.setChannel("email");
            x.setOptIn(true);
            return x;
        });

        if (channelOrNull != null) p.setChannel(channelOrNull);
        if (optInOrNull != null) p.setOptIn(optInOrNull);

        preferenceJpa.save(p);
    }

    @Override
    public Map<UUID, String> findFullNamesByIds(List<UUID> customerIds) {
        if (customerIds == null || customerIds.isEmpty()) return Map.of();

        return customerJpa.findAllById(customerIds).stream()
                .collect(Collectors.toMap(
                        e -> e.getId(),
                        e -> e.getFullname(),
                        (a, b) -> a
                ));
    }
}
