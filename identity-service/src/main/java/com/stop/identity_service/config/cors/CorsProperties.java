package com.stop.identity_service.config.cors;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * SPA / mobil istemci origin listesi. {@code application.yml} içindeki {@code identity.cors} ile doldurulur.
 */
@ConfigurationProperties(prefix = "identity.cors")
public record CorsProperties(List<String> allowedOriginPatterns) {

    public CorsProperties {
        if (allowedOriginPatterns == null || allowedOriginPatterns.isEmpty()) {
            allowedOriginPatterns = List.of(
                    "http://localhost:*",
                    "http://127.0.0.1:*",
                    "http://10.0.2.2:*",
                    "http://192.168.*:*"
            );
        }
    }
}
