package com.tagokoder.identity.infra.out.persistence.jpa.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@EqualsAndHashCode
public class IdentityLinkKey implements Serializable {
    @Column(name="identity_id")
    private UUID identityId;
    @Column(name="customer_id")
    private UUID customerId;
}
