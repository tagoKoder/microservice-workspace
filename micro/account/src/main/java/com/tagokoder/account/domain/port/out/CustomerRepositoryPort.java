package com.tagokoder.account.domain.port.out;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.tagokoder.account.domain.model.Customer;

public interface CustomerRepositoryPort {

    Customer save(Customer customer);

    Optional<Customer> findById(UUID id);

    void updateCoreFields(UUID id, String fullNameOrNull, String riskSegmentOrNull, String statusOrNull);

    void upsertContact(UUID customerId, String emailOrNull, String phoneOrNull);
    void upsertPreference(UUID customerId, String channelOrNull, Boolean optInOrNull);

    Map<UUID, String> findFullNamesByIds(List<UUID> customerIds);
}
