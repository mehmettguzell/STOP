package com.stop.communication_service.config.jwt;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static UUID getCurrentUserId() {
        Jwt jwt = getJwt();
        return UUID.fromString(jwt.getSubject());
    }

    public static String getCurrentUserEmail() {
        Jwt jwt = getJwt();
        return jwt.getClaim("email");
    }

    public static String getCurrentUserRole() {
        Jwt jwt = getJwt();
        return jwt.getClaim("role");
    }

    private static Jwt getJwt() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("No JWT authentication found");
        }

        return jwt;
    }
}
