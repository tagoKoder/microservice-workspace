package com.tagokoder.identity.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProps(
        String env,
        Aws aws,
        Security security
) {
    public record Aws(String region, String avpPolicyStoreId, String auditBusName) {}
    public record Security(String issuerUri, String audience, String hashSalt, String channel) {}
}
