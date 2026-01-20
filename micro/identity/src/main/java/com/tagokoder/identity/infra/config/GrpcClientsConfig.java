package com.tagokoder.identity.infra.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcClientsConfig {

  @Bean(destroyMethod = "shutdownNow")
  public ManagedChannel accountsChannel(IdentityProps props) {
    return ManagedChannelBuilder.forTarget(props.getAccountsTarget())
      .usePlaintext() // en prod: mTLS
      .build();
  }

  @Bean(destroyMethod = "shutdownNow")
  public ManagedChannel ledgerChannel(IdentityProps props) {
    return ManagedChannelBuilder.forTarget(props.getLedgerTarget())
      .usePlaintext()
      .build();
  }

  @Bean
  public bank.accounts.v1.CustomersServiceGrpc.CustomersServiceBlockingStub customersStub(ManagedChannel accountsChannel) {
    return bank.accounts.v1.CustomersServiceGrpc.newBlockingStub(accountsChannel);
  }

  @Bean
  public bank.accounts.v1.AccountsServiceGrpc.AccountsServiceBlockingStub accountsStub(ManagedChannel accountsChannel) {
    return bank.accounts.v1.AccountsServiceGrpc.newBlockingStub(accountsChannel);
  }

  @Bean
  public bank.ledgerpayments.v1.LedgerServiceGrpc.LedgerServiceBlockingStub ledgerStub(ManagedChannel ledgerChannel) {
    return bank.ledgerpayments.v1.LedgerServiceGrpc.newBlockingStub(ledgerChannel);
  }
}
