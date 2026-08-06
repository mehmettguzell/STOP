package com.stop.identity_service.userProfile.service.avatar;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.avatar-storage")
public record AvatarStorageProperties(String directory) {}
