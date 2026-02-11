package com.tagokoder.identity.application;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "identity.security")
@Data
public class IdentitySecurityProperties {
    private String refreshTokenPepper;
    private String refreshTokenEncKeyB64;
    private String accessTokenEncKeyB64;
}
