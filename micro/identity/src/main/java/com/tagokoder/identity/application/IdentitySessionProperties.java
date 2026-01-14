package com.tagokoder.identity.application;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "identity.session")
@Data
public class IdentitySessionProperties {
    private long ttlSeconds = 1800;
    private long absoluteTtlSeconds = 43200; // 12h por defecto
}
