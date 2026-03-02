package com.tagokoder.identity.infra.out.grpc;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.tagokoder.identity.domain.port.out.AccountsClientPort;
import com.tagokoder.identity.infra.security.grpc.BearerTokenCallCredentials;

// Ajusta imports de request/response según tu accounts.proto real:
import bank.accounts.v1.CreateCustomerRequest;
import bank.accounts.v1.CreateCustomerResponse;
import bank.accounts.v1.CustomersServiceGrpc;
import bank.accounts.v1.InternalAccountsServiceGrpc;
import bank.accounts.v1.OpenAccountWithOpeningBonusRequest;
import bank.accounts.v1.OpenAccountWithOpeningBonusResponse;
import bank.accounts.v1.ProductType;

@Component
public class AccountsGrpcClientAdapter implements AccountsClientPort {

  private final CustomersServiceGrpc.CustomersServiceBlockingStub customers;
  private final InternalAccountsServiceGrpc.InternalAccountsServiceBlockingStub internalStub;

  public AccountsGrpcClientAdapter(
    CustomersServiceGrpc.CustomersServiceBlockingStub customers,
    InternalAccountsServiceGrpc.InternalAccountsServiceBlockingStub internalStub
  ) {
    this.customers = customers;
    this.internalStub = internalStub;
  }

  @Override
  public String createCustomer(
    String bearerToken,
    String idempotencyKey,
    String externalRef,
    String fullName,
    LocalDate birthDate,
    String tin,
    String email,
    String phone
  ) {
    var stub = customers.withCallCredentials(new BearerTokenCallCredentials(bearerToken));

    CreateCustomerRequest req = CreateCustomerRequest.newBuilder()
      .setIdempotencyKey(idempotencyKey)
      .setExternalRef(externalRef)
      .setFullName(fullName)
      .setBirthDate(birthDate.toString())
      .setTin(tin == null ? "" : tin)
      .setEmail(email == null ? "" : email)
      .setPhone(phone == null ? "" : phone)
      .build();

    CreateCustomerResponse resp = stub.createCustomer(req);
    return resp.getCustomerId();
  }

    @Override
    public OpenedAccount openAccountWithOpeningBonus(
        String bearer,
        String customerId,
        String currency,
        String productType,
        String idempotencyKey,
        String externalRef,
        String initiatedBy
    ) {
      OpenAccountWithOpeningBonusRequest req = OpenAccountWithOpeningBonusRequest.newBuilder()
          .setCustomerId(customerId)
          .setProductType(mapProductType(productType))
          .setCurrency(currency)
          .setIdempotencyKey(idempotencyKey)
          .setExternalRef(externalRef)
          .setInitiatedBy(initiatedBy == null || initiatedBy.isBlank() ? "svc:identity" : initiatedBy)
          .build();

      OpenAccountWithOpeningBonusResponse resp = internalStub
          .withCallCredentials(new BearerTokenCallCredentials(bearer))
          .openAccountWithOpeningBonus(req);

      return new OpenedAccount(
          resp.getAccountId(),
          resp.getAccountNumber(),
          resp.getBonusJournalId(),
          resp.getStatus()
      );
    }

  private ProductType mapProductType(String productType) {
      if (productType == null) return ProductType.PRODUCT_TYPE_UNSPECIFIED;
  
      String s = productType.toUpperCase();
      return switch (s) {
      case "CHECKING", "PRODUCT_TYPE_CHECKING" -> ProductType.PRODUCT_TYPE_CHECKING;
      case "SAVINGS", "PRODUCT_TYPE_SAVINGS" -> ProductType.PRODUCT_TYPE_SAVINGS;
      default -> ProductType.PRODUCT_TYPE_UNSPECIFIED;
      };
  }
}
