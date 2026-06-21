package com.stop.match_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "match.cors")
public record CorsProperties(List<String> allowedOriginPatterns) {}
