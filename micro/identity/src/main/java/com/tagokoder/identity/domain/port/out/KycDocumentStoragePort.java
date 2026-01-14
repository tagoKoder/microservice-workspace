package com.tagokoder.identity.domain.port.out;

public interface KycDocumentStoragePort {

  StoredObject store(String registrationId, String type, byte[] content, String contentType);

  void delete(StoredObject obj);

  record StoredObject(String bucket, String key) {}
}
