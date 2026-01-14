package com.tagokoder.identity.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "identity.kyc.storage")
public class IdentityKycStorageProperties {

    private String type; // s3
    private String bucket;
    private String region;

    private String kmsKeyId; // opcional

    private String stagingPrefix = "staging/";
    private String finalPrefix = "kyc/";

    private long maxBytesIdFront = 5 * 1024 * 1024;
    private long maxBytesSelfie  = 5 * 1024 * 1024;
}
