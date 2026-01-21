package com.tagokoder.identity.infra.out.grpc;

import org.springframework.stereotype.Component;

import com.tagokoder.identity.domain.port.out.AccountsClientPort;
import com.tagokoder.identity.infra.security.grpc.BearerTokenCallCredentials;

import bank.accounts.v1.CustomersServiceGrpc;
import bank.accounts.v1.ProductType;
import bank.accounts.v1.AccountsServiceGrpc;
// Ajusta imports de request/response según tu accounts.proto real:
import bank.accounts.v1.CreateCustomerRequest;
import bank.accounts.v1.CreateCustomerResponse;
import bank.accounts.v1.CreateAccountRequest;
import bank.accounts.v1.CreateAccountResponse;

import java.time.LocalDate;

@Component
public class AccountsGrpcClientAdapter implements AccountsClientPort {

  private final CustomersServiceGrpc.CustomersServiceBlockingStub customers;
  private final AccountsServiceGrpc.AccountsServiceBlockingStub accounts;

  public AccountsGrpcClientAdapter(
    CustomersServiceGrpc.CustomersServiceBlockingStub customers,
    AccountsServiceGrpc.AccountsServiceBlockingStub accounts
  ) {
    this.customers = customers;
    this.accounts = accounts;
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
  public String createAccount(
    String bearerToken,
    String idempotencyKey,
    String externalRef,
    String customerId,
    String currency,
    String productType
  ) {
    var stub = accounts.withCallCredentials(new BearerTokenCallCredentials(bearerToken));

    CreateAccountRequest req = CreateAccountRequest.newBuilder()
      .setIdempotencyKey(idempotencyKey)
      .setExternalRef(externalRef)
      .setCustomerId(customerId)
      .setCurrency(currency)
      .setProductType(mapProductType(productType))
      .build();

    CreateAccountResponse resp = stub.createAccount(req);
    return resp.getAccountId();
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
