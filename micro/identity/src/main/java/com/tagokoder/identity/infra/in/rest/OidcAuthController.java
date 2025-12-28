package com.tagokoder.identity.infra.in.rest;

import com.tagokoder.identity.domain.port.in.CompleteLoginUseCase;
import com.tagokoder.identity.domain.port.in.StartLoginUseCase;
import com.tagokoder.identity.infra.in.api.model.OidcStartLoginRequestDto;
import com.tagokoder.identity.infra.in.api.model.OidcStartLoginResponseDto;
import com.tagokoder.identity.infra.in.api.model.OidcTokenRequestDto;
import com.tagokoder.identity.infra.in.api.model.OidcTokenResponseDto;
import com.tagokoder.identity.infra.in.api.model.OidcTokensDto;
import com.tagokoder.identity.infra.in.api.model.OidcUserDto;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Validated
@RequestMapping("/api/v1/oidc-auth")
public class OidcAuthController {

    private final StartLoginUseCase startLogin;
    private final CompleteLoginUseCase completeLogin;

    public OidcAuthController(StartLoginUseCase startLogin,
                              CompleteLoginUseCase completeLogin) {
        this.startLogin = startLogin;
        this.completeLogin = completeLogin;
    }

    @PostMapping("/identity/oidc/login")
    public ResponseEntity<OidcStartLoginResponseDto> startOidcLogin(OidcStartLoginRequestDto body) {
        var res = startLogin.start(new StartLoginUseCase.StartLoginCommand(
                body.getChannel(),
                body.getRedirectAfterLogin()
        ));
        var dto = new OidcStartLoginResponseDto();
        URI authorizationUri = URI.create(res.authorizationUrl());
        dto.setAuthorizationUrl(authorizationUri);
        dto.setState(res.state());
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/identity/oidc/token")
    public ResponseEntity<OidcTokenResponseDto> completeOidcLogin(OidcTokenRequestDto body) {
        var res = completeLogin.complete(new CompleteLoginUseCase.CompleteLoginCommand(
                body.getCode(),
                body.getState(),
                body.getIp(),
                body.getUserAgent(),
                body.getChannel()
        ));

        var userDto = new OidcUserDto();
        userDto.setName(res.name());
        userDto.setEmail(res.email());
        userDto.setRoles(res.roles());

        var tokensDto = new OidcTokensDto();
        tokensDto.setAccessToken(res.accessToken());
        tokensDto.setRefreshToken(res.refreshToken());
        tokensDto.setExpiresIn(res.expiresIn());

        var dto = new OidcTokenResponseDto();
        dto.setIdentityId(res.identityId());
        dto.setSubjectIdOidc(res.subjectIdOidc());
        dto.setProvider(res.provider());
        dto.setUser(userDto);
        dto.setTokens(tokensDto);
        dto.setRedirectAfterLogin(res.redirectAfterLogin());

        return ResponseEntity.ok(dto);
    }
}