// src/main/java/com/santiago_tumbaco/identity/model/DoctorInfo.java
package com.santiago_tumbaco.identity.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "doctor_info")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class DoctorInfo extends Base{

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId                                     // usa el mismo valor de PK que person.id
    @JoinColumn(name = "person_id")
    private Person person;

    // Catálogo remoto → guardar como BIGINT plano
    @Column(name = "specialty_id")
    private Long specialtyId;

    @Column(name = "license_number", length = 64)
    private String licenseNumber;

    @Lob
    private String bio;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
