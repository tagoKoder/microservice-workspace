package com.tagokoder.account.infra.in.grpc;

import com.tagokoder.account.domain.port.in.CreateCustomerUseCase;
import com.tagokoder.account.domain.port.in.PatchCustomerUseCase;
import com.google.protobuf.StringValue;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.time.LocalDate;

import bank.accounts.v1.*;

@Service
public class CustomersGrpcService extends CustomersServiceGrpc.CustomersServiceImplBase {

    private final CreateCustomerUseCase createCustomer;
    private final PatchCustomerUseCase patchCustomer;

    public CustomersGrpcService(CreateCustomerUseCase createCustomer, PatchCustomerUseCase patchCustomer) {
        this.createCustomer = createCustomer;
        this.patchCustomer = patchCustomer;
    }

    @Override
    public void createCustomer(CreateCustomerRequest request, StreamObserver<CreateCustomerResponse> responseObserver) {
        CreateCustomerUseCase.Address addr = null;
        if (request.hasAddress()) {
            var a = request.getAddress();
            addr = new CreateCustomerUseCase.Address(
                    a.getCountry(), a.getLine1(), a.getLine2(), a.getCity(), a.getProvince(), a.getPostalCode()
            );
        }

        var res = createCustomer.create(new CreateCustomerUseCase.Command(
                request.getFullName(),
                LocalDate.parse(request.getBirthDate()), // tú lo manejabas como string/date en OpenAPI; conserva tu lógica
                request.getTin(),
                request.getRiskSegment().name(),
                request.getEmail(),
                request.getPhone(),
                addr
        ));

        responseObserver.onNext(CreateCustomerResponse.newBuilder()
                .setCustomerId(res.customerId().toString())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void patchCustomer(PatchCustomerRequest request, StreamObserver<PatchCustomerResponse> responseObserver) {
        UUID id = UUID.fromString(request.getId());

        String fullName = request.hasFullName() ? request.getFullName().getValue() : null;

        String riskSegment = request.hasRiskSegment() ? request.getRiskSegment().getValue().name() : null;
        String customerStatus = request.hasCustomerStatus() ? request.getCustomerStatus().getValue().name() : null;

        PatchCustomerUseCase.ContactPatch contact = null;
        if (request.hasContact()) {
            var c = request.getContact();
            contact = new PatchCustomerUseCase.ContactPatch(
                    c.hasEmail() ? c.getEmail().getValue() : null,
                    c.hasPhone() ? c.getPhone().getValue() : null
            );
        }

        PatchCustomerUseCase.PreferencesPatch prefs = null;
        if (request.hasPreferences()) {
            var p = request.getPreferences();
            prefs = new PatchCustomerUseCase.PreferencesPatch(
                    p.hasChannel() ? p.getChannel().getValue().name() : null,
                    p.hasOptIn() ? p.getOptIn().getValue() : null
            );
        }

        var res = patchCustomer.patch(new PatchCustomerUseCase.Command(
                id,
                fullName,
                riskSegment,
                customerStatus,
                contact,
                prefs
        ));

        responseObserver.onNext(PatchCustomerResponse.newBuilder()
                .setCustomerId(res.customerId().toString())
                .build());
        responseObserver.onCompleted();
    }
}
