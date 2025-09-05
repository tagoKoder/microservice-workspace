// src/main/java/com/santiago_tumbaco/identity/model/UserAccount.java
package com.santiago_tumbaco.identity.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(
    name = "user_account",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_account_iss_sub", columnNames = {"idp_issuer", "idp_sub"})
    },
    indexes = {
        @Index(name = "ix_user_account_person", columnList = "person_id"),
        @Index(name = "ix_user_account_email",  columnList = "email")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserAccount extends Base{

    // Mucha gente permite varios accounts por persona (1:N)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(name = "idp_issuer", length = 255, nullable = false)
    private String idpIssuer;   // claim iss

    @Column(name = "idp_sub", length = 255, nullable = false)
    private String idpSub;      // claim sub

    @Column(length = 128)
    private String username;

    @Column(length = 255)
    private String email;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
