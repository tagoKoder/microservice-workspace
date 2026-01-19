package com.tagokoder.identity.domain.port.out;

import java.util.List;
import java.util.UUID;

import com.tagokoder.identity.domain.model.kyc.FinalizedObject;
import com.tagokoder.identity.domain.model.kyc.PresignedUpload;
import com.tagokoder.identity.domain.model.kyc.UploadedObject;

public interface KycPresignedStoragePort {

    List<PresignedUpload> issuePresignedUploads(
            UUID registrationId,
            String idFrontContentTypeHint,
            String selfieContentTypeHint
    );

    List<FinalizedObject> confirmAndFinalize(
            UUID registrationId,
            List<UploadedObject> objects
    );

    /** best-effort: cleanup staging keys if needed */
    void rollbackBestEffort(List<String> stagingKeys);
}
