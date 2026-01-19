package com.tagokoder.identity.infra.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tagokoder.identity.application.IdentityKycStorageProperties;
import com.tagokoder.identity.domain.port.out.KycPresignedStoragePort;
import com.tagokoder.identity.infra.out.storage.S3PresignedKycStorage;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(IdentityKycStorageProperties.class)
public class KycConfig {

    @Bean
    public S3Client s3Client(IdentityKycStorageProperties props) {
        return S3Client.builder()
                .region(Region.of(props.getRegion()))
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(IdentityKycStorageProperties props) {
        return S3Presigner.builder()
                .region(Region.of(props.getRegion()))
                .build();
    }

    @Bean
    public KycPresignedStoragePort kycPresignedStoragePort(
            S3Client s3,
            S3Presigner presigner,
            IdentityKycStorageProperties props
    ) {
        return new S3PresignedKycStorage(s3, presigner, props);
    }
}
