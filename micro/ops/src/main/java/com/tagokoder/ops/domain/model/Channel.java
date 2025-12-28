package com.tagokoder.ops.domain.model;

public enum Channel {
  EMAIL, SMS, PUSH;

  public static Channel fromWire(String v) {
    return switch (v.toLowerCase()) {
      case "email" -> EMAIL;
      case "sms" -> SMS;
      case "push" -> PUSH;
      default -> throw new IllegalArgumentException("invalid channel");
    };
  }

  public String toWire() {
    return switch (this) {
      case EMAIL -> "email";
      case SMS -> "sms";
      case PUSH -> "push";
    };
  }
}
