package com.tagokoder.identity.infra.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tagokoder.identity.application.IdentityClientsProperties;

import bank.accounts.v1.AccountsServiceGrpc;
import bank.accounts.v1.CustomersServiceGrpc;
import bank.accounts.v1.InternalAccountsServiceGrpc;
import bank.ledgerpayments.v1.LedgerServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

@Configuration
public class GrpcClientsConfig {

  @Bean(name = "accountsChannel", destroyMethod = "shutdownNow")
  public ManagedChannel accountsChannel(IdentityClientsProperties props) {
    return ManagedChannelBuilder.forTarget(props.getAccountsTarget())
      .usePlaintext()
      .build();
  }

  @Bean(name = "ledgerChannel", destroyMethod = "shutdownNow")
  public ManagedChannel ledgerChannel(IdentityClientsProperties props) {
    return ManagedChannelBuilder.forTarget(props.getLedgerTarget())
      .usePlaintext()
      .build();
  }

  @Bean
  public CustomersServiceGrpc.CustomersServiceBlockingStub customersStub(
      @Qualifier("accountsChannel") ManagedChannel accountsChannel
  ) {
    return CustomersServiceGrpc.newBlockingStub(accountsChannel);
  }

  @Bean
  public AccountsServiceGrpc.AccountsServiceBlockingStub accountsStub(
      @Qualifier("accountsChannel") ManagedChannel accountsChannel
  ) {
    return AccountsServiceGrpc.newBlockingStub(accountsChannel);
  }

  @Bean
  public InternalAccountsServiceGrpc.InternalAccountsServiceBlockingStub internalAccountsStub(
      @Qualifier("accountsChannel") ManagedChannel accountsChannel
  ) {
    return InternalAccountsServiceGrpc.newBlockingStub(accountsChannel);
  }

  @Bean
  public LedgerServiceGrpc.LedgerServiceBlockingStub ledgerStub(
      @Qualifier("ledgerChannel") ManagedChannel ledgerChannel
  ) {
    return LedgerServiceGrpc.newBlockingStub(ledgerChannel);
  }
}