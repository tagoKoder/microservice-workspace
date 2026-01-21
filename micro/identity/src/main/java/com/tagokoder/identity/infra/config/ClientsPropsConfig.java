package com.tagokoder.identity.infra.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.tagokoder.identity.application.IdentityClientsProperties;

@Configuration
@EnableConfigurationProperties(IdentityClientsProperties.class)
public class ClientsPropsConfig {}
