package com.tagokoder.account.infra.in.grpc.mapper;

import bank.accounts.v1.*;

public final class ProtoEnumMapper {
  private ProtoEnumMapper() {}

  public static String toDbRiskSegment(RiskSegment rs) {
    if (rs == null) return "low";
    return switch (rs) {
      case RISK_SEGMENT_LOW -> "low";
      case RISK_SEGMENT_MEDIUM -> "medium";
      case RISK_SEGMENT_HIGH -> "high";
      case RISK_SEGMENT_UNSPECIFIED, UNRECOGNIZED -> "low"; // default de negocio
    };
  }

  /** Para PATCH: si viene UNSPECIFIED, lo tratamos como "no modificar" */
  public static String toDbRiskSegmentOrNull(RiskSegment rs) {
    if (rs == null) return null;
    return switch (rs) {
      case RISK_SEGMENT_LOW -> "low";
      case RISK_SEGMENT_MEDIUM -> "medium";
      case RISK_SEGMENT_HIGH -> "high";
      case RISK_SEGMENT_UNSPECIFIED, UNRECOGNIZED -> null; // ignora
    };
  }

  public static String toDbCustomerStatusOrNull(CustomerStatus st) {
    if (st == null) return null;
    return switch (st) {
      case CUSTOMER_STATUS_ACTIVE -> "active";
      case CUSTOMER_STATUS_SUSPENDED -> "suspended";
      case CUSTOMER_STATUS_UNSPECIFIED, UNRECOGNIZED -> null;
    };
  }

  public static String mapProductType(ProductType pt) {
  return switch (pt) {
    case PRODUCT_TYPE_CHECKING -> "checking";
    case PRODUCT_TYPE_SAVINGS  -> "savings";
    // si tu proto tiene UNRECOGNIZED o DEFAULT:
    case PRODUCT_TYPE_UNSPECIFIED, UNRECOGNIZED -> throw new IllegalArgumentException("product_type required");
  };
}
}
