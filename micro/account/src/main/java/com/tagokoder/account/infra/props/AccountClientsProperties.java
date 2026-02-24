package com.tagokoder.account.infra.props;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix = "clients")
@Data
public class AccountClientsProperties {
    private String ledgerTarget;
    private String identityTarget;
}