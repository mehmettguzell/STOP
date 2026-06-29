package com.stop.api_gateway.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class GatewayAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        GatewayErrorCode code = map(authException);
        String correlationId = request.getHeader("X-Correlation-Id");
        GatewayErrorBody body = GatewayErrorBody.of(
                code,
                authException.getMessage() != null ? authException.getMessage() : code.name(),
                request.getRequestURI(),
                correlationId
        );
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private static GatewayErrorCode map(AuthenticationException ex) {
        if (ex instanceof OAuth2AuthenticationException oauth) {
            OAuth2Error err = oauth.getError();
            String errorCode = err != null ? err.getErrorCode() : null;
            if (errorCode != null) {
                if ("invalid_request".equalsIgnoreCase(errorCode)) {
                    return GatewayErrorCode.TOKEN_MISSING;
                }
                if ("invalid_token".equalsIgnoreCase(errorCode)) {
                    return mapJwtCause(oauth);
                }
            }
            String desc = err != null ? err.getDescription() : null;
            if (desc != null) {
                String d = desc.toLowerCase();
                if (d.contains("expired")) {
                    return GatewayErrorCode.TOKEN_EXPIRED;
                }
                if (d.contains("malformed")) {
                    return GatewayErrorCode.TOKEN_MALFORMED;
                }
                if (d.contains("signature")) {
                    return GatewayErrorCode.SIGNATURE_INVALID;
                }
            }
        }
        return mapJwtCause(ex);
    }

    private static GatewayErrorCode mapJwtCause(Throwable ex) {
        Throwable cur = ex;
        while (cur != null) {
            if (cur instanceof JwtException jwtEx) {
                String msg = jwtEx.getMessage();
                if (msg != null) {
                    String m = msg.toLowerCase();
                    if (m.contains("expired")) {
                        return GatewayErrorCode.TOKEN_EXPIRED;
                    }
                    if (m.contains("malformed")) {
                        return GatewayErrorCode.TOKEN_MALFORMED;
                    }
                    if (m.contains("signature")) {
                        return GatewayErrorCode.SIGNATURE_INVALID;
                    }
                }
                return GatewayErrorCode.TOKEN_INVALID;
            }
            cur = cur.getCause();
        }
        return GatewayErrorCode.TOKEN_MISSING;
    }
}
