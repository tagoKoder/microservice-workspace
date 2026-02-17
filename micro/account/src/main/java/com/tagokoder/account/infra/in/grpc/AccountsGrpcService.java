package com.tagokoder.account.infra.in.grpc;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.google.protobuf.Timestamp;
import com.tagokoder.account.application.service.IdempotencyService;
import com.tagokoder.account.domain.port.in.GetAccountBalancesUseCase;
import com.tagokoder.account.domain.port.in.GetAccountByNumberUseCase;
import com.tagokoder.account.domain.port.in.ListAccountsUseCase;
import com.tagokoder.account.domain.port.in.OpenAccountWithOpeningBonusUseCase;
import com.tagokoder.account.domain.port.in.PatchAccountLimitsUseCase;
import com.tagokoder.account.infra.in.grpc.mapper.ProtoEnumMapper;
import com.tagokoder.account.infra.security.grpc.IdempotencyKeyInterceptor;

import bank.accounts.v1.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
@GrpcService
public class AccountsGrpcService extends AccountsServiceGrpc.AccountsServiceImplBase {

    private final ListAccountsUseCase listAccounts;
    private final GetAccountByNumberUseCase getByNumber;
    private final GetAccountBalancesUseCase getBalances;
    private final PatchAccountLimitsUseCase patchLimits;
    private final IdempotencyService idem;
    private final OpenAccountWithOpeningBonusUseCase openAccountWithBonus;

    public AccountsGrpcService(
            ListAccountsUseCase listAccounts,
            GetAccountByNumberUseCase getByNumber,
            GetAccountBalancesUseCase getBalances,
            PatchAccountLimitsUseCase patchLimits,
            IdempotencyService idem,
            OpenAccountWithOpeningBonusUseCase openAccountWithBonus
    ) {
        this.listAccounts = listAccounts;
        this.getByNumber = getByNumber;
        this.getBalances = getBalances;
        this.patchLimits = patchLimits;
        this.idem = idem;
        this.openAccountWithBonus = openAccountWithBonus;
    }

        @Override
        public void listAccounts(ListAccountsRequest request, StreamObserver<ListAccountsResponse> responseObserver) {
        UUID customerId = UUID.fromString(request.getCustomerId());
        var res = listAccounts.listByCustomer(customerId);

        ListAccountsResponse.Builder out = ListAccountsResponse.newBuilder();
        res.accounts().forEach(a -> {
        AccountView.Builder view = AccountView.newBuilder()
        .setId(a.id().toString())
        .setCustomerId(a.customerId().toString())
        .setProductType(a.productType())
        .setCurrency(a.currency())
        .setStatus(a.status())
        .setOpenedAt(toTimestamp(a.openedAt()))
        .setUpdatedAt(toTimestamp(a.updatedAt()))
        .setBalances(AccountBalances.newBuilder()
                .setLedger(GrpcMoney.dbl(a.balances().ledger()))
                .setAvailable(GrpcMoney.dbl(a.balances().available()))
                .setHold(GrpcMoney.dbl(a.balances().hold()))
                .build());
        out.addAccounts(view.build());
        });

        responseObserver.onNext(out.build());
        responseObserver.onCompleted();
        }


    @Override
    public void getAccountByNumber(GetAccountByNumberRequest request,
                                StreamObserver<GetAccountByNumberResponse> responseObserver) {

        boolean includeInactive = request.hasIncludeInactive() && request.getIncludeInactive().getValue();

        var res = getByNumber.getByNumber(new GetAccountByNumberUseCase.Command(
                request.getAccountNumber(),
                includeInactive
        ));

        var a = res.account();

        responseObserver.onNext(GetAccountByNumberResponse.newBuilder()
                .setAccount(AccountLookup.newBuilder()
                        .setAccountId(a.accountId().toString())
                        .setCustomerId(a.customerId().toString())
                        .setAccountNumber(a.accountNumber())
                        .setDisplayName(a.displayName())
                        .setCurrency(a.currency())
                        .setStatus(a.status())
                        .setProductType(ProtoEnumMapper.mapProductTypeToEnum(a.productType()))
                        .build())
                .build());

        responseObserver.onCompleted();
    }

    @Override
    public void createAccount(CreateAccountRequest request, StreamObserver<CreateAccountResponse> responseObserver) {

        var res = openAccountWithBonus.open(new OpenAccountWithOpeningBonusUseCase.Command(
                UUID.fromString(request.getCustomerId()),
                ProtoEnumMapper.mapProductType(request.getProductType()),
                request.getCurrency(),
                request.getIdempotencyKey(),
                "system"
        ));

        responseObserver.onNext(CreateAccountResponse.newBuilder()
                .setAccountId(res.accountId().toString())
                .setAccountNumber(res.accountNumber() == null ? "" : res.accountNumber())
                .build());
        responseObserver.onCompleted();
    }

        @Override
        public void getAccountBalances(GetAccountBalancesRequest request, StreamObserver<GetAccountBalancesResponse> responseObserver) {
        var res = getBalances.get(UUID.fromString(request.getId()));

        responseObserver.onNext(GetAccountBalancesResponse.newBuilder()
        .setAccountId(res.accountId().toString())
        .setBalances(AccountBalances.newBuilder()
        .setLedger(GrpcMoney.dbl(res.ledger()))
        .setAvailable(GrpcMoney.dbl(res.available()))
        .setHold(GrpcMoney.dbl(res.hold()))
        .build())
        .build());
        responseObserver.onCompleted();
        }

        @Override
        public void patchAccountLimits(PatchAccountLimitsRequest request, StreamObserver<PatchAccountLimitsResponse> responseObserver) {

                String key = IdempotencyKeyInterceptor.IDEMPOTENCY_KEY.get(); // puede ser null

                // 1) si ya existe, responde lo mismo sin re-ejecutar
                var cached = idem.tryGet(key, "accounts.patchLimits", PatchAccountLimitsUseCase.Result.class);
                if (cached.isPresent()) {
                var res = cached.get();
                responseObserver.onNext(PatchAccountLimitsResponse.newBuilder()
                .setAccountId(res.accountId().toString())
                .setDailyOut(GrpcMoney.dbl(res.dailyOut()))
                .setDailyIn(GrpcMoney.dbl(res.dailyIn()))
                .build());
                responseObserver.onCompleted();
                return;
                }

                // 2) ejecuta caso normal
                var res = patchLimits.patch(new PatchAccountLimitsUseCase.Command(
                UUID.fromString(request.getId()),
                request.hasDailyOut() ? GrpcMoney.bd(request.getDailyOut().getValue()) : null,
                request.hasDailyIn()  ? GrpcMoney.bd(request.getDailyIn().getValue())  : null
                ));

                // 3) guarda respuesta idempotente
                idem.save(key, "accounts.patchLimits", 200, res);

                responseObserver.onNext(PatchAccountLimitsResponse.newBuilder()
                .setAccountId(res.accountId().toString())
                .setDailyOut(GrpcMoney.dbl(res.dailyOut()))
                .setDailyIn(GrpcMoney.dbl(res.dailyIn()))
                .build());
                responseObserver.onCompleted();
        }

    private static Timestamp toTimestamp(OffsetDateTime i) {
        if (i == null) return Timestamp.getDefaultInstance();
        return Timestamp.newBuilder()
                .setSeconds(i.toEpochSecond())
                .setNanos(i.getNano())
                .build();
    }
}
