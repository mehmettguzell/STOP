package com.stop.communication_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "communication.cors")
public record CorsProperties(List<String> allowedOriginPatterns) {}
