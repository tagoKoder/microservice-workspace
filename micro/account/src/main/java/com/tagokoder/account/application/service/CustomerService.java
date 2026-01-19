package com.tagokoder.account.application.service;

import com.tagokoder.account.domain.model.Customer;
import com.tagokoder.account.domain.port.in.CreateCustomerUseCase;
import com.tagokoder.account.domain.port.in.PatchCustomerUseCase;
import com.tagokoder.account.domain.port.out.AuditPort;
import com.tagokoder.account.domain.port.out.CustomerRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;


@Service
public class CustomerService implements CreateCustomerUseCase, PatchCustomerUseCase {

    private final CustomerRepositoryPort customerRepo;

    public CustomerService(CustomerRepositoryPort customerRepo) {
        this.customerRepo = customerRepo;
    }

    @Override
    @Transactional
    public CreateCustomerUseCase.Result create(CreateCustomerUseCase.Command c) {
        Customer customer = new Customer();
        customer.setFullName(c.fullName());
        customer.setBirthDate(c.birthDate());
        customer.setTin(c.tin());
        customer.setRiskSegment(c.riskSegment() != null ? c.riskSegment() : "low");
        customer.setCustomerStatus("active");

        Customer saved = customerRepo.save(customer);

        // contact + address + preferences por defecto
        customerRepo.upsertContact(saved.getId(), c.email(), c.phone());

        if (c.addressOrNull() != null) {
            // si quieres, agrega un método customerRepo.insertAddress(...) en el port
            // o maneja addresses en un AddressRepositoryPort separado.
            // (mantengo simple aquí para no extender demasiado)
        }

        customerRepo.upsertPreference(saved.getId(), "email", true);

        return new CreateCustomerUseCase.Result(saved.getId());
    }

    @Override
    @Transactional
    public PatchCustomerUseCase.Result patch(PatchCustomerUseCase.Command c) {
        // Validaciones mínimas
        if (c.riskSegmentOrNull() != null &&
                !c.riskSegmentOrNull().matches("low|medium|high")) {
            throw new IllegalArgumentException("Invalid riskSegment");
        }
        if (c.customerStatusOrNull() != null &&
                !c.customerStatusOrNull().matches("active|suspended")) {
            throw new IllegalArgumentException("Invalid customerStatus");
        }

        customerRepo.updateCoreFields(c.customerId(), c.fullNameOrNull(), c.riskSegmentOrNull(), c.customerStatusOrNull());

        if (c.contactOrNull() != null) {
            customerRepo.upsertContact(c.customerId(), c.contactOrNull().emailOrNull(), c.contactOrNull().phoneOrNull());
        }
        if (c.preferencesOrNull() != null) {
            customerRepo.upsertPreference(c.customerId(), c.preferencesOrNull().channelOrNull(), c.preferencesOrNull().optInOrNull());
        }

        return new PatchCustomerUseCase.Result(c.customerId());
    }
}
