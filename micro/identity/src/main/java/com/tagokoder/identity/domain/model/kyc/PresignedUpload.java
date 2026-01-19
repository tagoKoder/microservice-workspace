package com.tagokoder.identity.domain.model.kyc;
import java.time.Instant;
import java.util.List;

import lombok.Builder;

@Builder
public record PresignedUpload(
        KycDocumentKind kind,
        String bucket,
        String key,
        String putUrl,
        Instant expiresAt,
        long maxBytes,
        String requiredContentType,
        List<UploadHeader> requiredHeaders
) {}
