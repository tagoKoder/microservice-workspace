package com.tagokoder.identity.infra.out.storage;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.tagokoder.identity.application.IdentityKycStorageProperties;
import com.tagokoder.identity.domain.model.kyc.FinalizedObject;
import com.tagokoder.identity.domain.model.kyc.KycDocumentKind;
import com.tagokoder.identity.domain.model.kyc.PresignedUpload;
import com.tagokoder.identity.domain.model.kyc.UploadHeader;
import com.tagokoder.identity.domain.model.kyc.UploadedObject;
import com.tagokoder.identity.domain.port.out.KycPresignedStoragePort;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.MetadataDirective;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

public class S3PresignedKycStorage implements KycPresignedStoragePort {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final IdentityKycStorageProperties props;

    public S3PresignedKycStorage(S3Client s3, S3Presigner presigner, IdentityKycStorageProperties props) {
        this.s3 = Objects.requireNonNull(s3);
        this.presigner = Objects.requireNonNull(presigner);
        this.props = Objects.requireNonNull(props);
    }

    @Override
    public List<PresignedUpload> issuePresignedUploads(UUID registrationId, String idFrontHint, String selfieHint) {
        require(registrationId != null, "registrationId required");

        String idCt = pickContentType(props.getIdFrontAllowedContentTypes(), idFrontHint, props.getIdFrontDefaultContentType());
        String sfCt = pickContentType(props.getSelfieAllowedContentTypes(), selfieHint, props.getSelfieDefaultContentType());

        PresignedUpload idFront = presignPut(registrationId, KycDocumentKind.ID_FRONT, idCt, props.getIdFrontMaxBytes());
        PresignedUpload selfie  = presignPut(registrationId, KycDocumentKind.SELFIE,  sfCt, props.getSelfieMaxBytes());

        return List.of(idFront, selfie);
    }

    @Override
    public List<FinalizedObject> confirmAndFinalize(UUID registrationId, List<UploadedObject> objects) {
        require(registrationId != null, "registrationId required");
        require(objects != null && !objects.isEmpty(), "objects required");

        UploadedObject idFront = find(objects, KycDocumentKind.ID_FRONT);
        UploadedObject selfie  = find(objects, KycDocumentKind.SELFIE);

        FinalizedObject fin1 = confirmOne(registrationId, idFront, props.getIdFrontMaxBytes(), props.getIdFrontAllowedContentTypes());
        FinalizedObject fin2 = confirmOne(registrationId, selfie,  props.getSelfieMaxBytes(),  props.getSelfieAllowedContentTypes());

        return List.of(fin1, fin2);
    }

    @Override
    public void rollbackBestEffort(List<String> stagingKeys) {
        if (stagingKeys == null || stagingKeys.isEmpty()) return;
        for (String key : stagingKeys) {
            if (key == null || key.isBlank()) continue;
            try {
                s3.deleteObject(DeleteObjectRequest.builder()
                        .bucket(props.getBucket())
                        .key(key)
                        .build());
            } catch (Exception ignored) {
                // best-effort
            }
        }
    }

    // ---------------- Internals ----------------

    private PresignedUpload presignPut(UUID registrationId, KycDocumentKind kind, String contentType, long maxBytes) {
        String bucket = must(props.getBucket(), "identity.kyc.bucket is required");
        String stagingKey = buildStagingKey(registrationId, kind, contentType);

        PutObjectRequest.Builder put = PutObjectRequest.builder()
                .bucket(bucket)
                .key(stagingKey)
                .contentType(contentType);

        List<UploadHeader> headers = new ArrayList<>();
        headers.add(new UploadHeader("Content-Type", contentType));

        // SSE-KMS optional
        if (props.getKmsKeyId() != null && !props.getKmsKeyId().isBlank()) {
            put.serverSideEncryption(ServerSideEncryption.AWS_KMS);
            put.ssekmsKeyId(props.getKmsKeyId());

            headers.add(new UploadHeader("x-amz-server-side-encryption", "aws:kms"));
            headers.add(new UploadHeader("x-amz-server-side-encryption-aws-kms-key-id", props.getKmsKeyId()));
        }

        Duration ttl = Duration.ofSeconds(props.getPresignExpiresSeconds());
        PresignedPutObjectRequest presigned = presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .signatureDuration(ttl)
                        .putObjectRequest(put.build())
                        .build()
        );

        URL url = presigned.url();
        Instant expiresAt = Instant.now().plus(ttl);

        return new PresignedUpload(
                kind,
                bucket,
                stagingKey,
                url.toString(),
                expiresAt,
                maxBytes,
                contentType,
                headers
        );
    }

    private FinalizedObject confirmOne(UUID registrationId, UploadedObject obj, long maxBytes, Set<String> allowedTypes) {
        require(obj != null, "uploaded object required");
        require(notBlank(obj.bucket()), "bucket required");
        require(notBlank(obj.key()), "key required");
        require(obj.kind() != null, "kind required");

        // bucket must match configured bucket
        String expectedBucket = must(props.getBucket(), "identity.kyc.bucket is required");
        if (!expectedBucket.equals(obj.bucket())) {
            throw new IllegalArgumentException("bucket mismatch");
        }

        // key must be inside stagingPrefix/registrationId/
        String requiredPrefix = normalizePrefix(props.getStagingPrefix()) + registrationId + "/";
        if (!obj.key().startsWith(requiredPrefix)) {
            throw new IllegalArgumentException("invalid staging key prefix");
        }
        if (obj.key().contains("..")) {
            throw new IllegalArgumentException("invalid key");
        }

        HeadObjectResponse head;
        try {
            head = s3.headObject(HeadObjectRequest.builder().bucket(obj.bucket()).key(obj.key()).build());
        } catch (SdkException e) {
            throw new IllegalStateException("S3 headObject failed: " + e.getMessage(), e);
        }

        long len = head.contentLength() == null ? -1 : head.contentLength();
        if (len <= 0 || len > maxBytes) {
            throw new IllegalArgumentException("invalid content-length: " + len);
        }

        String ct = head.contentType() == null ? "" : head.contentType().toLowerCase(Locale.ROOT).trim();
        if (!ct.isBlank() && allowedTypes != null && !allowedTypes.isEmpty()) {
            if (!allowedTypes.contains(ct)) {
                throw new IllegalArgumentException("content-type not allowed: " + ct);
            }
        }

        // best-effort etag validation (may vary for multipart; keep for small uploads)
        if (notBlank(obj.etag()) && notBlank(head.eTag())) {
            String expected = stripQuotes(obj.etag());
            String actual = stripQuotes(head.eTag());
            if (!actual.equalsIgnoreCase(expected)) {
                throw new IllegalArgumentException("etag mismatch");
            }
        }

        String finalKey = buildFinalKey(registrationId, obj.kind(), ct);

        // COPY (final) + DELETE (staging)
        CopyObjectRequest.Builder copy = CopyObjectRequest.builder()
                .copySource(obj.bucket() + "/" + obj.key())
                .destinationBucket(obj.bucket())
                .destinationKey(finalKey)
                .metadataDirective(MetadataDirective.COPY);

        if (props.getKmsKeyId() != null && !props.getKmsKeyId().isBlank()) {
            copy.serverSideEncryption(ServerSideEncryption.AWS_KMS);
            copy.ssekmsKeyId(props.getKmsKeyId());
        }

        s3.copyObject(copy.build());
        s3.deleteObject(DeleteObjectRequest.builder().bucket(obj.bucket()).key(obj.key()).build());

        return new FinalizedObject(
            obj.kind(),
            obj.bucket(),
            finalKey,
            stripQuotes(head.eTag()),
            len,
            ct
        );
    }

    private UploadedObject find(List<UploadedObject> objects, KycDocumentKind kind) {
        return objects.stream()
                .filter(o -> o != null && o.kind() == kind)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("missing object kind=" + kind));
    }

    private String buildStagingKey(UUID registrationId, KycDocumentKind kind, String contentType) {
        String prefix = normalizePrefix(props.getStagingPrefix());
        String ext = extensionFrom(contentType);
        return prefix + registrationId + "/" + kind.name().toLowerCase(Locale.ROOT) + "/" + UUID.randomUUID() + ext;
    }

    private String buildFinalKey(UUID registrationId, KycDocumentKind kind, String contentType) {
        String prefix = normalizePrefix(props.getFinalPrefix());
        String ext = extensionFrom(contentType);
        return prefix + registrationId + "/" + kind.name().toLowerCase(Locale.ROOT) + "/" + UUID.randomUUID() + ext;
    }

    private String extensionFrom(String ct) {
        if (ct == null) return "";
        String x = ct.toLowerCase(Locale.ROOT);
        if (x.contains("jpeg")) return ".jpg";
        if (x.contains("png"))  return ".png";
        if (x.contains("pdf"))  return ".pdf";
        return "";
    }

    private String pickContentType(Set<String> allowed, String hint, String fallback) {
        if (!notBlank(hint)) return fallback;
        String normalized = hint.toLowerCase(Locale.ROOT).trim();
        if (allowed != null && allowed.contains(normalized)) return normalized;
        return fallback;
    }

    private String normalizePrefix(String p) {
        String x = (p == null) ? "" : p.trim();
        while (x.startsWith("/")) x = x.substring(1);
        while (x.endsWith("/")) x = x.substring(0, x.length() - 1);
        return x.isEmpty() ? "" : (x + "/");
    }

    private String stripQuotes(String s) {
        String x = s.trim();
        if (x.startsWith("\"") && x.endsWith("\"") && x.length() >= 2) {
            return x.substring(1, x.length() - 1);
        }
        return x;
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private void require(boolean cond, String msg) {
        if (!cond) throw new IllegalArgumentException(msg);
    }

    private String must(String v, String msg) {
        if (v == null || v.isBlank()) throw new IllegalStateException(msg);
        return v;
    }
}
