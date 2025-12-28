package com.tagokoder.account.infra.in.grpc;

import com.tagokoder.account.domain.port.in.CreateAccountUseCase;
import com.tagokoder.account.domain.port.in.GetAccountBalancesUseCase;
import com.tagokoder.account.domain.port.in.ListAccountsUseCase;
import com.tagokoder.account.domain.port.in.PatchAccountLimitsUseCase;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import bank.accounts.v1.*; // si usas java_package recomendado
// si NO defines java_package, cambia a: import bank.accounts.v1.*;
@Service
public class AccountsGrpcService extends AccountsServiceGrpc.AccountsServiceImplBase {

    private final ListAccountsUseCase listAccounts;
    private final CreateAccountUseCase createAccount;
    private final GetAccountBalancesUseCase getBalances;
    private final PatchAccountLimitsUseCase patchLimits;

    public AccountsGrpcService(
            ListAccountsUseCase listAccounts,
            CreateAccountUseCase createAccount,
            GetAccountBalancesUseCase getBalances,
            PatchAccountLimitsUseCase patchLimits
    ) {
        this.listAccounts = listAccounts;
        this.createAccount = createAccount;
        this.getBalances = getBalances;
        this.patchLimits = patchLimits;
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
                            .setLedger(a.balances().ledger())
                            .setAvailable(a.balances().available())
                            .setHold(a.balances().hold())
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
                        .setLedger(res.ledger())
                        .setAvailable(res.available())
                        .setHold(res.hold())
                        .build())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void patchAccountLimits(PatchAccountLimitsRequest request, StreamObserver<PatchAccountLimitsResponse> responseObserver) {
        UUID accountId = UUID.fromString(request.getId());

        Double dailyOut = request.hasDailyOut() ? request.getDailyOut().getValue() : null;
        Double dailyIn  = request.hasDailyIn()  ? request.getDailyIn().getValue()  : null;

        var res = patchLimits.patch(new PatchAccountLimitsUseCase.Command(
                accountId,
                dailyOut,
                dailyIn
        ));

        responseObserver.onNext(PatchAccountLimitsResponse.newBuilder()
                .setAccountId(res.accountId().toString())
                .setDailyOut(res.dailyOut())
                .setDailyIn(res.dailyIn())
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
