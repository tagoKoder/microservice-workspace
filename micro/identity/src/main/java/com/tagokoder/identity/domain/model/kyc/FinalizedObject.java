package com.tagokoder.identity.domain.model.kyc;


public record FinalizedObject(
        KycDocumentKind kind,
        String bucket,
        String key
) {}
