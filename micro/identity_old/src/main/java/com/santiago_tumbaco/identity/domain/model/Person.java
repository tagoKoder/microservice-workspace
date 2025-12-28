package com.santiago_tumbaco.identity.domain.model;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.cglib.core.Local;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "person")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Person extends Base{
    @Column(name="government_id", unique=true)
    private String governmentId;
    private String name;
    private String lastName;
    @Column(nullable=false, unique=true)
    private String email;
    private String phone;
    private String gender;
    private LocalDate birthDate;
    private String address;
    @Column(name = "created_at", columnDefinition = "timestamptz", updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", columnDefinition = "timestamptz")
    private Instant updatedAt;
    @Column(name = "deleted_at", columnDefinition = "timestamptz")
    private Instant deletedAt;
    
}
