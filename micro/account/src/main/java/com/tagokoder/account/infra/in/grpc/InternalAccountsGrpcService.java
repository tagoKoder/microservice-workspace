package com.tagokoder.account.infra.in.grpc;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.google.protobuf.StringValue;
import com.tagokoder.account.domain.port.in.ReleaseHoldUseCase;
import com.tagokoder.account.domain.port.in.ReserveHoldUseCase;
import com.tagokoder.account.domain.port.in.ValidateAccountsAndLimitsUseCase;

import bank.accounts.v1.InternalAccountsServiceGrpc;
import bank.accounts.v1.ReleaseHoldRequest;
import bank.accounts.v1.ReleaseHoldResponse;
import bank.accounts.v1.ReserveHoldRequest;
import bank.accounts.v1.ReserveHoldResponse;
import bank.accounts.v1.ValidateAccountsAndLimitsRequest;
import bank.accounts.v1.ValidateAccountsAndLimitsResponse;
import io.grpc.stub.StreamObserver;

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
        GrpcMoney.bd(request.getAmount())
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
        GrpcMoney.bd(request.getHold().getAmount()),
        reason
        ));

        responseObserver.onNext(ReserveHoldResponse.newBuilder()
        .setOk(res.ok())
        .setNewHold(GrpcMoney.dbl(res.newHold()))
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
        GrpcMoney.bd(request.getHold().getAmount()),
        reason
        ));

        responseObserver.onNext(ReleaseHoldResponse.newBuilder()
        .setOk(res.ok())
        .setNewHold(GrpcMoney.dbl(res.newHold()))
        .build());
        responseObserver.onCompleted();
        }
}
