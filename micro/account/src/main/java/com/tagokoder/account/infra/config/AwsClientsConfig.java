package com.tagokoder.account.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.verifiedpermissions.VerifiedPermissionsClient;

@Configuration
public class AwsClientsConfig {

    @Bean
    VerifiedPermissionsClient verifiedPermissionsClient(AppProps props) {
        return VerifiedPermissionsClient.builder()
                .region(Region.of(props.aws().region()))
                .build();
    }

    @Bean
    EventBridgeClient eventBridgeClient(AppProps props) {
        return EventBridgeClient.builder()
                .region(Region.of(props.aws().region()))
                .build();
    }
    @Bean
    SqsClient sqsClient(AppProps props) {
        return SqsClient.builder().region(Region.of(props.aws().region())).build();
    }

}
