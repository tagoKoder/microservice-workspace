package com.tagokoder.account.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tagokoder.account.infra.props.AccountClientsProperties;

import bank.ledgerpayments.v1.LedgerServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

@Configuration
public class GrpcClientsConfig {

    @Bean(destroyMethod = "shutdownNow")
    public ManagedChannel ledgerChannel(AccountClientsProperties props) {
        return ManagedChannelBuilder.forTarget(props.getLedgerTarget())
                .usePlaintext() // prod: mTLS
                .build();
    }

    @Bean
    public LedgerServiceGrpc.LedgerServiceBlockingStub ledgerStub(ManagedChannel ledgerChannel) {
        return LedgerServiceGrpc.newBlockingStub(ledgerChannel);
    }
}