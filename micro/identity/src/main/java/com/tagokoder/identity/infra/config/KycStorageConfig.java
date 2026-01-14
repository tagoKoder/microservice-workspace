package com.tagokoder.identity.infra.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tagokoder.identity.application.IdentityKycStorageProperties;
import com.tagokoder.identity.domain.port.out.KycDocumentStoragePort;
import com.tagokoder.identity.infra.out.storage.S3KycDocumentStorage;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@EnableConfigurationProperties(IdentityKycStorageProperties.class)
public class KycStorageConfig {

    @Bean
    public S3Client s3Client(IdentityKycStorageProperties props) {
        return S3Client.builder()
                .region(Region.of(props.getRegion()))
                .build();
    }

    @Bean
    public KycDocumentStoragePort kycDocumentStoragePort(S3Client s3, IdentityKycStorageProperties props) {
        if (!"s3".equalsIgnoreCase(props.getType())) {
            throw new IllegalStateException("Unsupported KYC storage type: " + props.getType());
        }
        return new S3KycDocumentStorage(s3, props);
    }
}
