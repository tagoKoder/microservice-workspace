package com.tagokoder.identity.domain.port.in;

import java.util.UUID;

public interface WebauthnMfaUseCase {

    record BeginRegistrationCommand(UUID identityId, String deviceName) {}
    record BeginRegistrationResponse(String requestId, String optionsJson) {}

    record FinishRegistrationCommand(String requestId, String credentialJson) {}

    record BeginAssertionCommand(UUID identityId, UUID sessionId) {}
    record BeginAssertionResponse(String requestId, String optionsJson) {}

    record FinishAssertionCommand(UUID sessionId, String requestId, String credentialJson) {}

    BeginRegistrationResponse beginRegistration(BeginRegistrationCommand c);

    void finishRegistration(FinishRegistrationCommand c);

    BeginAssertionResponse beginAssertion(BeginAssertionCommand c);

    void finishAssertion(FinishAssertionCommand c);
}