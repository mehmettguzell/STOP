package com.stop.communication_service.common.exception;

import tools.jackson.databind.ObjectMapper;
import com.stop.communication_service.common.error.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, resolveMessage(ex));
        pd.setProperty("code", CommonErrorCode.UNAUTHORIZED.getCode());
        pd.setInstance(URI.create(request.getRequestURI()));

        objectMapper.writeValue(response.getWriter(), pd);
    }

    private String resolveMessage(AuthenticationException ex) {
        Throwable root = getRootCause(ex);
        if (root instanceof JwtException jwt) {
            String msg = jwt.getMessage();
            if (msg != null && msg.toLowerCase().contains("expired")) return "Oturum suresi doldu, lutfen tekrar giris yapin.";
            return "Gecersiz oturum, lutfen tekrar giris yapin.";
        }
        return CommonErrorCode.UNAUTHORIZED.getMessage();
    }

    private Throwable getRootCause(Throwable t) {
        while (t.getCause() != null) t = t.getCause();
        return t;
    }
}
