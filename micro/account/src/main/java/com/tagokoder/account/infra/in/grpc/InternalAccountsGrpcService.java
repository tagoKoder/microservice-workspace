package com.tagokoder.account.infra.in.grpc;

import com.tagokoder.account.domain.port.in.*;
import com.google.protobuf.StringValue;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

import java.util.UUID;

import bank.accounts.v1.*;

@Service
public class InternalAccountsGrpcService extends InternalAccountsServiceGrpc.InternalAccountsServiceImplBase {

    private final ValidateAccountsAndLimitsUseCase validateUC;
    private final ReserveHoldUseCase reserveUC;
    private final ReleaseHoldUseCase releaseUC;

    public InternalAccountsGrpcService(
            ValidateAccountsAndLimitsUseCase validateUC,
            ReserveHoldUseCase reserveUC,
            ReleaseHoldUseCase releaseUC
    ) {
        this.validateUC = validateUC;
        this.reserveUC = reserveUC;
        this.releaseUC = releaseUC;
    }

    @Override
    public void validateAccountsAndLimits(ValidateAccountsAndLimitsRequest request,
                                         StreamObserver<ValidateAccountsAndLimitsResponse> responseObserver) {

        var res = validateUC.validate(new ValidateAccountsAndLimitsUseCase.Command(
                UUID.fromString(request.getSourceAccountId()),
                UUID.fromString(request.getDestinationAccountId()),
                request.getCurrency(),
                request.getAmount()
        ));

        ValidateAccountsAndLimitsResponse.Builder out = ValidateAccountsAndLimitsResponse.newBuilder()
                .setOk(res.ok());

        if (!res.ok() && res.reasonOrNull() != null) {
            out.setReason(StringValue.of(res.reasonOrNull()));
        }

        responseObserver.onNext(out.build());
        responseObserver.onCompleted();
    }

    @Override
    public void reserveHold(ReserveHoldRequest request, StreamObserver<ReserveHoldResponse> responseObserver) {
        UUID id = UUID.fromString(request.getId());

        String reason = null;
        if (request.getHold().hasReason()) reason = request.getHold().getReason().getValue();

        var res = reserveUC.reserve(new ReserveHoldUseCase.Command(
                id,
                request.getHold().getCurrency(),
                request.getHold().getAmount(),
                reason
        ));

        responseObserver.onNext(ReserveHoldResponse.newBuilder()
                .setOk(res.ok())
                .setNewHold(res.newHold())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void releaseHold(ReleaseHoldRequest request, StreamObserver<ReleaseHoldResponse> responseObserver) {
        UUID id = UUID.fromString(request.getId());

        String reason = null;
        if (request.getHold().hasReason()) reason = request.getHold().getReason().getValue();

        var res = releaseUC.release(new ReleaseHoldUseCase.Command(
                id,
                request.getHold().getCurrency(),
                request.getHold().getAmount(),
                reason
        ));

        responseObserver.onNext(ReleaseHoldResponse.newBuilder()
                .setOk(res.ok())
                .setNewHold(res.newHold())
                .build());
        responseObserver.onCompleted();
    }
}
