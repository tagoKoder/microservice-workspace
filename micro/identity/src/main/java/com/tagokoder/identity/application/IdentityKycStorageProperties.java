package com.tagokoder.identity.application;

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@ConfigurationProperties(prefix = "identity.kyc")
@Data
public class IdentityKycStorageProperties {

    // S3
    private String bucket;
    private String region;
    private String kmsKeyId; // optional

    // Prefixes
    private String stagingPrefix = "staging";
    private String finalPrefix = "kyc";

    // TTL
    private long presignExpiresSeconds = 900;

    // Limits
    private long idFrontMaxBytes = 8L * 1024 * 1024;
    private long selfieMaxBytes = 6L * 1024 * 1024;

    // Content types allow-list
    private Set<String> idFrontAllowedContentTypes = Set.of("image/jpeg", "image/png", "application/pdf");
    private Set<String> selfieAllowedContentTypes  = Set.of("image/jpeg", "image/png");

    // Defaults
    private String idFrontDefaultContentType = "image/jpeg";
    private String selfieDefaultContentType  = "image/jpeg";

}
