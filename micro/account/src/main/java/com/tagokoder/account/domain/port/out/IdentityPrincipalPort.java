package com.tagokoder.account.domain.port.out;

import java.util.List;

public interface IdentityPrincipalPort {
  PrincipalInfo resolvePrincipal(String bearerAccessToken, boolean requireLink);

  record PrincipalInfo(
      String subjectIdOidc,
      String principalId,
      String customerId,
      List<String> roles,
      String userStatus
  ) {}
}