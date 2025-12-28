package com.tagokoder.identity.infra.out.persistence.jpa;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
public class IdentityLinkKey implements Serializable {
    private UUID identityId;
    private UUID customerId;
}
