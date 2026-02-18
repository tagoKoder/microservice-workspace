package com.tagokoder.account.infra.in.grpc;

import com.tagokoder.account.domain.port.in.CreateCustomerUseCase;
import com.tagokoder.account.domain.port.in.PatchCustomerUseCase;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import com.tagokoder.account.infra.in.grpc.mapper.ProtoEnumMapper;


import java.util.UUID;
import java.time.LocalDate;
import bank.accounts.v1.*;
import static com.tagokoder.account.infra.in.grpc.validation.CustomersGrpcValidators.*;

@GrpcService
public class CustomersGrpcService extends CustomersServiceGrpc.CustomersServiceImplBase {

    private final CreateCustomerUseCase createCustomer;
    private final PatchCustomerUseCase patchCustomer;

    public CustomersGrpcService(CreateCustomerUseCase createCustomer, PatchCustomerUseCase patchCustomer) {
        this.createCustomer = createCustomer;
        this.patchCustomer = patchCustomer;
    }

    @Override
    public void createCustomer(CreateCustomerRequest request, StreamObserver<CreateCustomerResponse> responseObserver) {
        var cmd = toCreateCustomerCommand(request);

        var res = createCustomer.create(cmd);

        responseObserver.onNext(CreateCustomerResponse.newBuilder()
                .setCustomerId(res.customerId().toString())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void patchCustomer(PatchCustomerRequest request, StreamObserver<PatchCustomerResponse> responseObserver) {
        var cmd = toPatchCustomerCommand(request);

        var res = patchCustomer.patch(cmd);

        responseObserver.onNext(PatchCustomerResponse.newBuilder()
                .setCustomerId(res.customerId().toString())
                .build());
        responseObserver.onCompleted();
    }
}
