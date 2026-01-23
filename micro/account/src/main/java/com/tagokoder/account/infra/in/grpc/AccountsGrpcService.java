package com.tagokoder.account.infra.in.grpc;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.google.protobuf.Timestamp;
import com.tagokoder.account.application.service.IdempotencyService;
import com.tagokoder.account.domain.port.in.CreateAccountUseCase;
import com.tagokoder.account.domain.port.in.GetAccountBalancesUseCase;
import com.tagokoder.account.domain.port.in.ListAccountsUseCase;
import com.tagokoder.account.domain.port.in.PatchAccountLimitsUseCase;

// si usas java_package recomendado
import bank.accounts.v1.AccountBalances;
import bank.accounts.v1.AccountView;
import bank.accounts.v1.AccountsServiceGrpc;
import bank.accounts.v1.CreateAccountRequest;
import bank.accounts.v1.CreateAccountResponse;
import bank.accounts.v1.GetAccountBalancesRequest;
import bank.accounts.v1.GetAccountBalancesResponse;
import bank.accounts.v1.ListAccountsRequest;
import bank.accounts.v1.ListAccountsResponse;
import bank.accounts.v1.PatchAccountLimitsRequest;
import bank.accounts.v1.PatchAccountLimitsResponse;
import io.grpc.stub.StreamObserver;
// si NO defines java_package, cambia a: import bank.accounts.v1.*;
@Service
public class AccountsGrpcService extends AccountsServiceGrpc.AccountsServiceImplBase {

    private final ListAccountsUseCase listAccounts;
    private final CreateAccountUseCase createAccount;
    private final GetAccountBalancesUseCase getBalances;
    private final PatchAccountLimitsUseCase patchLimits;
    private final IdempotencyService idem;

    public AccountsGrpcService(
            ListAccountsUseCase listAccounts,
            CreateAccountUseCase createAccount,
            GetAccountBalancesUseCase getBalances,
            PatchAccountLimitsUseCase patchLimits,
                IdempotencyService idem
    ) {
        this.listAccounts = listAccounts;
        this.createAccount = createAccount;
        this.getBalances = getBalances;
        this.patchLimits = patchLimits;
        this.idem = idem;
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
    public void createAccount(CreateAccountRequest request, StreamObserver<CreateAccountResponse> responseObserver) {
        var res = createAccount.create(new CreateAccountUseCase.Command(
                UUID.fromString(request.getCustomerId()),
                request.getProductType().name(), // tu dominio esperaba string
                request.getCurrency()
        ));

        responseObserver.onNext(CreateAccountResponse.newBuilder()
                .setAccountId(res.accountId().toString())
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
