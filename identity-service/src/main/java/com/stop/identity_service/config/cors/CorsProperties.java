package com.stop.identity_service.config.cors;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "identity.cors")
public record CorsProperties(List<String> allowedOriginPatterns) {}
