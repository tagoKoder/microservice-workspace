package com.tagokoder.identity.infra.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.tagokoder.identity.application.AppProps;
import com.tagokoder.identity.application.IdentityClientsProperties;

@Configuration
@EnableConfigurationProperties({AppProps.class,  IdentityClientsProperties.class})
public class PropsConfig {}
