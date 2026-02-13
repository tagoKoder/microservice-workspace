package com.tagokoder.account.infra.props;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "clients")
public class AccountClientsProperties {
    private String ledgerTarget;

    public String getLedgerTarget() { return ledgerTarget; }
    public void setLedgerTarget(String ledgerTarget) { this.ledgerTarget = ledgerTarget; }
}