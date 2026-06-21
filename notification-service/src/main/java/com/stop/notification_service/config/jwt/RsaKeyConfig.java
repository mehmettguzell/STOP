package com.stop.notification_service.config.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
public class RsaKeyConfig {

    @Value("${JWT_PUBLIC_KEY:}")
    private String publicKeyEnv;

    @Bean
    public RSAPublicKey jwtPublicKey() {
        try {
            if (publicKeyEnv == null || publicKeyEnv.isBlank()) {
                throw new IllegalStateException("JWT_PUBLIC_KEY env var tanımlı değil");
            }
            String key = publicKeyEnv
                    .replace("\\n", "\n")
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            return (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(key)));
        } catch (Exception ex) {
            throw new IllegalStateException("RSA public key yüklenemedi: " + ex.getMessage(), ex);
        }
    }
}
