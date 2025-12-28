package com.tagokoder.identity.domain.port.out;

import java.io.InputStream;

public interface KycDocumentStoragePort {

    String store(String registrationId, String type, InputStream content);

    // type: "ID_FRONT", "SELFIE"
}
