package com.tagokoder.account.infra.out.grpc;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.tagokoder.account.domain.port.out.LedgerClientPort;

import bank.ledgerpayments.v1.LedgerServiceGrpc;
import bank.ledgerpayments.v1.Ledgerpayments.CreditAccountRequest;
import bank.ledgerpayments.v1.Ledgerpayments.CreditAccountResponse;

@Component
public class LedgerGrpcClientAdapter implements LedgerClientPort {

    private final LedgerServiceGrpc.LedgerServiceBlockingStub ledger;

    public LedgerGrpcClientAdapter(LedgerServiceGrpc.LedgerServiceBlockingStub ledger) {
        this.ledger = ledger;
    }

    @Override
    public String creditAccount(
            String idempotencyKey,
            UUID accountId,
            String currency,
            String amount,
            String initiatedBy,
            String externalRef,
            String reason,
            UUID customerId
    ) {
        CreditAccountRequest req = CreditAccountRequest.newBuilder()
                .setIdempotencyKey(idempotencyKey)
                .setAccountId(accountId.toString())
                .setCurrency(currency)
                .setAmount(amount)
                .setInitiatedBy(initiatedBy == null ? "system" : initiatedBy)
                .setExternalRef(externalRef == null ? "" : externalRef)
                .setReason(reason == null ? "" : reason)
                .setCustomerId(customerId == null ? "" : customerId.toString())
                .build();

        CreditAccountResponse resp = ledger.creditAccount(req);
        return resp.getJournalId();
    }
}