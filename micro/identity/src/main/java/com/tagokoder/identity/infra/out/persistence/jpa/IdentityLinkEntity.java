package com.tagokoder.identity.infra.out.persistence.jpa;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name="identity_links")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IdentityLinkEntity {
    @EmbeddedId
    private IdentityLinkKey id;
}
