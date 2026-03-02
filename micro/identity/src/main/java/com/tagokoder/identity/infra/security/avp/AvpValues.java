package com.tagokoder.identity.infra.security.avp;

import java.util.List;

import software.amazon.awssdk.services.verifiedpermissions.model.AttributeValue;
import software.amazon.awssdk.services.verifiedpermissions.model.EntityIdentifier;

public final class AvpValues {
  private AvpValues() {}

  public static AttributeValue str(String v) {
    return AttributeValue.builder().string(v).build();
  }

  public static AttributeValue bool(boolean v) {
    return AttributeValue.builder().booleanValue(v).build();
  }

  public static AttributeValue lng(long v) {
    return AttributeValue.builder().longValue(v).build();
  }

  public static AttributeValue setStr(List<String> vals) {
    var list = vals.stream().map(x -> AttributeValue.builder().string(x).build()).toList();
    return AttributeValue.builder().set(list).build();
  }

  public static AttributeValue entity(String type, String id) {
    var eid = EntityIdentifier.builder().entityType(type).entityId(id).build();
    return AttributeValue.builder().entityIdentifier(eid).build();
  }
}