package com.stop.identity_service.config.jwt;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RefreshTokenGenerator {
    public String generate() {
        return UUID.randomUUID().toString()
                + UUID.randomUUID().toString();
    }
}
