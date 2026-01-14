package com.tagokoder.identity.domain.port.out;

import java.time.Duration;

public interface OidcStateRepositoryPort {

    void saveState(String state, String codeVerifier, String redirectAfterLogin, String nonce, Duration ttl);

    OidcState loadAndRemove(String state);

    record OidcState(String codeVerifier, String redirectAfterLogin, String nonce) {}
}
