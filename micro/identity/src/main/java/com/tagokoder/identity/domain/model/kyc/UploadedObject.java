package com.tagokoder.identity.domain.model.kyc;


public record UploadedObject(
        KycDocumentKind kind,
        String bucket,
        String key,
        String etag,
        Long contentLength,
        String contentType
) {}