package com.tagokoder.identity.application;


import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@ConfigurationProperties(prefix = "identity.oidc")
@Data
public class OidcProperties {

    private String bffCallbackUrl;
    private String provider;

    private String issuer;
    private String authUrl;
    private String tokenUrl;
    private String userInfoUrl;
    private String parUrl;        // opcional /application/o/par/
    private String clientId;
    private String clientSecret;

    private boolean usePar = false;
}
