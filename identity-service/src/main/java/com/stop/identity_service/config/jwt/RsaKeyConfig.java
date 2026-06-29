package com.stop.identity_service.config.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
public class RsaKeyConfig {

    @Value("${JWT_PUBLIC_KEY:}")
    private String publicKeyEnv;

    @Value("${JWT_PRIVATE_KEY:}")
    private String privateKeyEnv;

    @Bean
    public KeyPair keyPair() {
        try {
            return new KeyPair(loadPublicKey(), loadPrivateKey());
        } catch (Exception ex) {
            throw new IllegalStateException("RSA key yüklenemedi: " + ex.getMessage(), ex);
        }
    }

    private RSAPublicKey loadPublicKey() throws Exception {
        if (publicKeyEnv == null || publicKeyEnv.isBlank()) {
            throw new IllegalStateException("JWT_PUBLIC_KEY env var tanımlı değil");
        }
        String key = normalize(publicKeyEnv, "-----BEGIN PUBLIC KEY-----", "-----END PUBLIC KEY-----");
        return (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(key)));
    }

    private RSAPrivateKey loadPrivateKey() throws Exception {
        if (privateKeyEnv == null || privateKeyEnv.isBlank()) {
            throw new IllegalStateException("JWT_PRIVATE_KEY env var tanımlı değil");
        }
        String key = normalize(privateKeyEnv, "-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----");
        return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(key)));
    }

    private String normalize(String pem, String header, String footer) {
        return pem
                .replace("\\n", "\n")
                .replace(header, "")
                .replace(footer, "")
                .replaceAll("\\s", "");
    }
}
