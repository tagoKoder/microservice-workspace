package com.tagokoder.identity.infra.out.storage;

import java.util.Locale;
import java.util.UUID;

import com.tagokoder.identity.application.IdentityKycStorageProperties;
import com.tagokoder.identity.domain.port.out.KycDocumentStoragePort;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

public class S3KycDocumentStorage implements KycDocumentStoragePort {

    private final S3Client s3;
    private final IdentityKycStorageProperties props;

    public S3KycDocumentStorage(S3Client s3, IdentityKycStorageProperties props) {
        this.s3 = s3;
        this.props = props;
    }

    @Override
    public StoredObject store(String registrationId, String type, byte[] content, String contentType) {
        if (registrationId == null || registrationId.isBlank()) throw new IllegalArgumentException("registrationId required");
        if (type == null || type.isBlank()) throw new IllegalArgumentException("type required");
        if (content == null || content.length == 0) throw new IllegalArgumentException("content required");

        validateSize(type, content.length);

        String safeType = type.trim().toLowerCase(Locale.ROOT);
        String ext = extensionFromContentType(contentType);

        // 1) PUT a staging/
        String stagingKey = joinKey(props.getStagingPrefix(), registrationId + "/" + safeType + "/" + UUID.randomUUID() + ext);
        putPrivateObject(props.getBucket(), stagingKey, content, contentType);

        // 2) COPY a kyc/
        String finalKey = joinKey(props.getFinalPrefix(), registrationId + "/" + safeType + "/" + UUID.randomUUID() + ext);
        copyObjectPrivate(props.getBucket(), stagingKey, props.getBucket(), finalKey);

        // 3) DELETE staging/
        delete(new StoredObject(props.getBucket(), stagingKey));

        // 4) Retorna referencia FINAL (NO URL pública)
        return new StoredObject(props.getBucket(), finalKey);
    }

    @Override
    public void delete(StoredObject obj) {
        if (obj == null) return;
        if (obj.bucket() == null || obj.bucket().isBlank()) return;
        if (obj.key() == null || obj.key().isBlank()) return;

        try {
            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(obj.bucket())
                    .key(obj.key())
                    .build());
        } catch (NoSuchKeyException ignored) {
            // idempotente
        }
    }

    // ----------------- helpers -----------------

    private void validateSize(String type, int bytes) {
        String t = type.toUpperCase(Locale.ROOT);
        if ("ID_FRONT".equals(t) && bytes > props.getMaxBytesIdFront()) {
            throw new IllegalArgumentException("ID_FRONT exceeds max bytes: " + bytes);
        }
        if ("SELFIE".equals(t) && bytes > props.getMaxBytesSelfie()) {
            throw new IllegalArgumentException("SELFIE exceeds max bytes: " + bytes);
        }
    }

    private void putPrivateObject(String bucket, String key, byte[] content, String contentType) {
        PutObjectRequest.Builder b = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType != null ? contentType : "application/octet-stream");

        // SSE-KMS (opcional)
        if (props.getKmsKeyId() != null && !props.getKmsKeyId().isBlank()) {
            b.serverSideEncryption(ServerSideEncryption.AWS_KMS)
             .ssekmsKeyId(props.getKmsKeyId());
        }

        s3.putObject(b.build(), RequestBody.fromBytes(content));
    }

    private void copyObjectPrivate(String srcBucket, String srcKey, String dstBucket, String dstKey) {
        CopyObjectRequest.Builder b = CopyObjectRequest.builder()
                .copySource(srcBucket + "/" + srcKey)
                .destinationBucket(dstBucket)
                .destinationKey(dstKey)
                // mantenemos metadata/headers del destino controlados
                .metadataDirective(MetadataDirective.COPY);

        // SSE-KMS (opcional)
        if (props.getKmsKeyId() != null && !props.getKmsKeyId().isBlank()) {
            b.serverSideEncryption(ServerSideEncryption.AWS_KMS)
             .ssekmsKeyId(props.getKmsKeyId());
        }

        s3.copyObject(b.build());
    }

    private String joinKey(String prefix, String rest) {
        String p = (prefix == null) ? "" : prefix.trim();
        if (!p.isEmpty() && !p.endsWith("/")) p = p + "/";
        while (rest.startsWith("/")) rest = rest.substring(1);
        return p + rest;
    }

    private String extensionFromContentType(String ct) {
        if (ct == null) return "";
        String x = ct.toLowerCase(Locale.ROOT);
        if (x.contains("jpeg")) return ".jpg";
        if (x.contains("png"))  return ".png";
        if (x.contains("pdf"))  return ".pdf";
        return "";
    }
}
