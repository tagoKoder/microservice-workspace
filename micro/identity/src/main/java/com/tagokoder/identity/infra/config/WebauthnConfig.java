package com.tagokoder.identity.infra.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tagokoder.identity.application.WebauthnProperties;
import com.tagokoder.identity.domain.port.out.WebauthnCredentialRepositoryPort;
import com.tagokoder.identity.infra.webauthn.YubicoCredentialRepository;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;

@Configuration
@EnableConfigurationProperties(WebauthnProperties.class)
public class WebauthnConfig {

    @Bean
    public YubicoCredentialRepository yubicoCredentialRepository(WebauthnCredentialRepositoryPort repo) {
        return new YubicoCredentialRepository(repo);
    }

    @Bean
    public RelyingParty relyingParty(WebauthnProperties props, YubicoCredentialRepository credRepo) {
        RelyingPartyIdentity rpIdentity = RelyingPartyIdentity.builder()
            .id(props.getRpId())
            .name(props.getRpName())
            .build();

        return RelyingParty.builder()
            .identity(rpIdentity)
            .credentialRepository(credRepo)
            // Aquí usas directamente los strings de tus origins
            .origins(new java.util.HashSet<>(props.getOrigins()))
            .build();
    }

}
