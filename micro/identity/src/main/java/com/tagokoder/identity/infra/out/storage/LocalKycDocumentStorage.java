package com.tagokoder.identity.infra.out.storage;

import com.tagokoder.identity.domain.port.out.KycDocumentStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class LocalKycDocumentStorage implements KycDocumentStoragePort {

    @Value("${identity.kyc.storage-root:kyc-storage}")
    private String rootDir;

    @Override
    public String store(String registrationId, String type, InputStream content) {
        try {
            Path dir = Path.of(rootDir, registrationId);
            Files.createDirectories(dir);
            String filename = type.toLowerCase() + ".jpg";
            Path filePath = dir.resolve(filename);
            try (FileOutputStream out = new FileOutputStream(filePath.toFile())) {
                content.transferTo(out);
            }
            // URL lógica (para ahora puede ser path relativo, luego será URL pública de S3)
            return "/kyc/" + registrationId + "/" + filename;
        } catch (Exception e) {
            throw new RuntimeException("Error saving KYC document", e);
        }
    }
}
