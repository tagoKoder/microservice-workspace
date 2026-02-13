package com.tagokoder.account.infra.in.grpc;

import java.util.UUID;

import com.google.protobuf.StringValue;
import com.tagokoder.account.domain.port.in.BatchGetAccountSummariesUseCase;
import com.tagokoder.account.domain.port.in.ReleaseHoldUseCase;
import com.tagokoder.account.domain.port.in.ReserveHoldUseCase;
import com.tagokoder.account.domain.port.in.ValidateAccountsAndLimitsUseCase;
import com.tagokoder.account.infra.in.grpc.mapper.ProtoEnumMapper;

import bank.accounts.v1.InternalAccountsServiceGrpc;
import bank.accounts.v1.MissingAccount;
import bank.accounts.v1.ReleaseHoldRequest;
import bank.accounts.v1.ReleaseHoldResponse;
import bank.accounts.v1.ReserveHoldRequest;
import bank.accounts.v1.ReserveHoldResponse;
import bank.accounts.v1.ValidateAccountsAndLimitsRequest;
import bank.accounts.v1.ValidateAccountsAndLimitsResponse;
import bank.accounts.v1.AccountSummary;
import bank.accounts.v1.BatchGetAccountSummariesRequest;
import bank.accounts.v1.BatchGetAccountSummariesResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import io.grpc.Status;

@GrpcService
public class InternalAccountsGrpcService extends InternalAccountsServiceGrpc.InternalAccountsServiceImplBase {

    private final ValidateAccountsAndLimitsUseCase validateUC;
    private final ReserveHoldUseCase reserveUC;
    private final ReleaseHoldUseCase releaseUC;
    private final BatchGetAccountSummariesUseCase batchSummariesUC;

    public InternalAccountsGrpcService(
            ValidateAccountsAndLimitsUseCase validateUC,
            ReserveHoldUseCase reserveUC,
            ReleaseHoldUseCase releaseUC,
            BatchGetAccountSummariesUseCase batchSummariesUC
    ) {
        this.validateUC = validateUC;
        this.reserveUC = reserveUC;
        this.releaseUC = releaseUC;
        this.batchSummariesUC = batchSummariesUC;
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

    @Override
    public void batchGetAccountSummaries(
            BatchGetAccountSummariesRequest request,
            StreamObserver<BatchGetAccountSummariesResponse> responseObserver) {

        // guard simple (demo)
        if (request.getAccountIdsCount() > 200) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT.withDescription("max 200 account_ids").asRuntimeException()
            );
            return;
        }

        boolean includeInactive = request.hasIncludeInactive() && request.getIncludeInactive().getValue();

        // parse UUIDs (si alguno es inválido, lo mandamos a missing)
        var ids = new java.util.ArrayList<java.util.UUID>();
        var missingEarly = new java.util.ArrayList<MissingAccount>();

        for (String raw : request.getAccountIdsList()) {
            try {
                ids.add(java.util.UUID.fromString(raw));
            } catch (Exception ex) {
                missingEarly.add(MissingAccount.newBuilder()
                        .setAccountId(raw == null ? "" : raw)
                        .setReason(StringValue.of("invalid_uuid"))
                        .build());
            }
        }

        var res = batchSummariesUC.batchGetSummaries(
                new BatchGetAccountSummariesUseCase.Command(ids, includeInactive)
        );

        BatchGetAccountSummariesResponse.Builder out = BatchGetAccountSummariesResponse.newBuilder();

        // accounts OK
        for (var a : res.accounts()) {
            out.addAccounts(AccountSummary.newBuilder()
                    .setAccountId(a.accountId().toString())
                    .setAccountNumber(a.accountNumber())
                    .setProductType(ProtoEnumMapper.mapProductTypeToEnum(a.productType())) // NUEVO mapper
                    .setCurrency(a.currency())
                    .setStatus(a.status())
                    .setDisplayName(a.displayName() == null ? "" : a.displayName())
                    .build());
        }

        // missing (invalid_uuid + not_found/inactive)
        for (var m : missingEarly) out.addMissing(m);

        for (var m : res.missing()) {
            MissingAccount.Builder mb = MissingAccount.newBuilder()
                    .setAccountId(m.accountId().toString());
            if (m.reason() != null && !m.reason().isBlank()) {
                mb.setReason(StringValue.of(m.reason()));
            }
            out.addMissing(mb.build());
        }

        responseObserver.onNext(out.build());
        responseObserver.onCompleted();
    }
}
