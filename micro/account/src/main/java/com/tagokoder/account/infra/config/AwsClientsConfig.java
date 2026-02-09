package com.tagokoder.account.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.verifiedpermissions.VerifiedPermissionsClient;

@Configuration
public class AwsClientsConfig {

    @Bean
    AwsCredentialsProvider awsCredentialsProvider() {
        // Local: toma AWS_PROFILE + ~/.aws/*
        // ECS: toma Task Role (container credentials)
        return DefaultCredentialsProvider.create();
    }

    @Bean
    VerifiedPermissionsClient verifiedPermissionsClient(AppProps props, AwsCredentialsProvider creds) {
        return VerifiedPermissionsClient.builder()
                .region(Region.of(props.aws().region()))
                .credentialsProvider(creds)
                .build();
    }

    @Bean
    EventBridgeClient eventBridgeClient(AppProps props, AwsCredentialsProvider creds) {
        return EventBridgeClient.builder()
                .region(Region.of(props.aws().region()))
                .credentialsProvider(creds)
                .build();
    }

    @Bean
    SqsClient sqsClient(AppProps props, AwsCredentialsProvider creds) {
        return SqsClient.builder()
                .region(Region.of(props.aws().region()))
                .credentialsProvider(creds)
                .build();
    }
}
