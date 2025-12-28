package com.tagokoder.identity.domain.port.out;

import java.time.Duration;

public interface OidcStateRepositoryPort {

    void saveState(String state, String codeVerifier, String redirectAfterLogin, Duration ttl);

    OidcState loadAndRemove(String state);

    class OidcState {
        public final String codeVerifier;
        public final String redirectAfterLogin;

        public OidcState(String codeVerifier, String redirectAfterLogin) {
            this.codeVerifier = codeVerifier;
            this.redirectAfterLogin = redirectAfterLogin;
        }
    }
}

