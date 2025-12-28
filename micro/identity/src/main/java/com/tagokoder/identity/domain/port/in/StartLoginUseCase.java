package com.tagokoder.identity.domain.port.in;

public interface StartLoginUseCase {

    StartLoginResponse start(StartLoginCommand command);
    record StartLoginCommand(String channel, String redirectAfterLogin) {}
    record StartLoginResponse(String authorizationUrl, String state) {}
}