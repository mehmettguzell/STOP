package com.stop.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
public class RsaPublicKeyConfig {

    @Value("${JWT_PUBLIC_KEY:}")
    private String publicKeyEnv;

    @Bean
    public RSAPublicKey jwtPublicKey() throws Exception {
        if (publicKeyEnv == null || publicKeyEnv.isBlank()) {
            throw new IllegalStateException("JWT_PUBLIC_KEY env var tanımlı değil");
        }
        String key = publicKeyEnv
                .replace("\\n", "\n")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(key);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
    }
}
