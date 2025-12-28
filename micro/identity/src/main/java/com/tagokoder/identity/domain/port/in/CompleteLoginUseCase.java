package com.tagokoder.identity.domain.port.in;
import java.util.List;
import java.util.UUID;

public interface CompleteLoginUseCase {

    CompleteLoginResponse complete(CompleteLoginCommand command);

    record CompleteLoginCommand(
            String code,
            String state,
            String ip,
            String userAgent,
            String channel
    ) {}

    record CompleteLoginResponse(
            UUID identityId,
            String subjectIdOidc,
            String provider,
            String name,
            String email,
            List<String> roles,
            String accessToken,
            String refreshToken,
            long expiresIn,
            String redirectAfterLogin
    ) {}
}