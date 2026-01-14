package com.tagokoder.identity.domain.port.out;

import java.util.List;

import lombok.Data;

public interface OidcIdpClientPort {
    String buildAuthorizationUrl(String state, String nonce, String codeChallenge, String redirectUri);

    TokenResult exchangeCodeForTokens(String code, String codeVerifier, String redirectUri);

    TokenResult refreshTokens(String refreshToken);

    void revokeRefreshToken(String refreshToken);

    UserInfoResult fetchUserInfo(String accessToken);
    class TokenResult {
        public final String accessToken;
        public final String refreshToken;
        public final long expiresIn;
        public final String idToken;

        public TokenResult(String accessToken, String refreshToken, long expiresIn, String idToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresIn = expiresIn;
            this.idToken = idToken;
        }
    }

    class UserInfoResult {
        public final String subject;
        public final String email;
        public final String name;
        public final List<String> groups;
        public UserInfoResult(String subject, String email, String name, List<String> groups) {
            this.subject = subject;
            this.email = email;
            this.name = name;
            this.groups = groups;
        }
    }
}