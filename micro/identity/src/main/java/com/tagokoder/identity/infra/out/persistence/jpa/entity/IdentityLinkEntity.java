package com.tagokoder.identity.infra.out.persistence.jpa.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "identity_links")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdentityLinkEntity {

  @Id
  @Column(name = "identity_id", nullable = false)
  private UUID identityId;

  @Column(name = "customer_id", nullable = false, length = 64, unique = true)
  private String customerId;
}