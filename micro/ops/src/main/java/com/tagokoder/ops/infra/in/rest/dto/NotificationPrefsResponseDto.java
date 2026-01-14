package com.tagokoder.ops.infra.in.rest.dto;

import java.util.List;

public record NotificationPrefsResponseDto(List<PrefDto> prefs) {
  public record PrefDto(String channel, boolean opt_in, String updated_at) {}
}
