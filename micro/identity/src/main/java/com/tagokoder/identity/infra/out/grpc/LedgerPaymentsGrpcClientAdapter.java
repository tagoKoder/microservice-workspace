package com.tagokoder.identity.infra.out.grpc;

import org.springframework.stereotype.Component;

import com.tagokoder.identity.domain.port.out.LedgerPaymentsClientPort;
import com.tagokoder.identity.infra.security.grpc.BearerTokenCallCredentials;

import bank.ledgerpayments.v1.LedgerServiceGrpc;
// Ajusta imports según tu ledgerpayments.proto real:
import bank.ledgerpayments.v1.Ledgerpayments.CreditAccountRequest;
import bank.ledgerpayments.v1.Ledgerpayments.CreditAccountResponse;

@Component
public class LedgerPaymentsGrpcClientAdapter implements LedgerPaymentsClientPort {

  private final LedgerServiceGrpc.LedgerServiceBlockingStub ledger;

  public LedgerPaymentsGrpcClientAdapter(LedgerServiceGrpc.LedgerServiceBlockingStub ledger) {
    this.ledger = ledger;
  }

  @Override
  public String creditAccount(
    String bearerToken,
    String idempotencyKey,
    String accountId,
    String currency,
    String amount,
    String reason,
    String customerId
  ) {
    var stub = ledger.withCallCredentials(new BearerTokenCallCredentials(bearerToken));

    CreditAccountRequest req = CreditAccountRequest.newBuilder()
      .setIdempotencyKey(idempotencyKey)
      .setAccountId(accountId)
      .setCurrency(currency)
      .setAmount(amount)
      .setCustomerId(customerId == null ? "" : customerId)
      .setReason(reason == null ? "" : reason)
      .setInitiatedBy(customerId)
      
      .build();

    CreditAccountResponse resp = stub.creditAccount(req);
    return resp.getJournalId();
  }
}
