package com.tagokoder.account.infra.out.persistence.jpa.entity;


import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "customer_contacts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerContactEntity {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;
    @Column(name="customer_id", nullable=false)
    private UUID customerId;
    @ManyToOne
    @JoinColumn(name="customerId", insertable=false, updatable=false)
    private CustomerEntity customer;
    private String email;
    @Column(name="email_verified")
    private boolean emailVerified;
    private String phone;
}
