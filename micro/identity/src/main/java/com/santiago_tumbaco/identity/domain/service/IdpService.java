package com.santiago_tumbaco.identity.domain.service;

import org.springframework.security.oauth2.jwt.Jwt;

import com.santiago_tumbaco.identity.domain.dto.WhoAmIResult;

public interface IdpService {
    WhoAmIResult linkFromIdToken(Jwt jwt);
    WhoAmIResult whoAmIFromAccessToken(Jwt jwt);
    Jwt verifyJWT(String rawJwt);
}
