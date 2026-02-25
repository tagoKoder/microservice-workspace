package com.tagokoder.account.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tagokoder.account.infra.props.AccountClientsProperties;

import bank.identity.v1.*;
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

    @Bean(destroyMethod = "shutdownNow")
    public ManagedChannel identityChannel(AccountClientsProperties props) {
        String target = props.getIdentityTarget();
        if (target == null || target.isBlank()) {
            throw new IllegalStateException("Missing account.clients.identityTarget (e.g. identity:9090) for profile");
        }
        return ManagedChannelBuilder.forTarget(target)
            .usePlaintext()
            .build();
    }

    @Bean
    public LedgerServiceGrpc.LedgerServiceBlockingStub ledgerStub(ManagedChannel ledgerChannel) {
        return LedgerServiceGrpc.newBlockingStub(ledgerChannel);
    }

    @Bean
    public PrincipalServiceGrpc.PrincipalServiceBlockingStub principalStub(ManagedChannel identityChannel) {
        return PrincipalServiceGrpc.newBlockingStub(identityChannel);
    }
}