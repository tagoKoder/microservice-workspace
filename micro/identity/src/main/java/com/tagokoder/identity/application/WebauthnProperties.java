package com.tagokoder.identity.application;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@ConfigurationProperties(prefix = "identity.webauthn")
@Data
public class WebauthnProperties {
    private String rpId;     // ej: "tu-dominio.com"
    private String rpName;   // ej: "ImaginaryBank"
    private List<String> origins; // ej: ["https://app.tu-dominio.com"]
}
