package com.stop.notification_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "notification.cors")
public record CorsProperties(List<String> allowedOriginPatterns) {}
