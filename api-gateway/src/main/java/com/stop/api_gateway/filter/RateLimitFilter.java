package com.stop.api_gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.stop.api_gateway.config.GatewayProperties;
import com.stop.api_gateway.error.GatewayErrorBody;
import com.stop.api_gateway.error.GatewayErrorCode;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final GatewayProperties gatewayProperties;
    private final ObjectMapper objectMapper;
    private final LoadingCache<String, Bucket> rateLimitCache;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (!gatewayProperties.getRateLimit().isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = buildClientKey(request);
        Bucket bucket = rateLimitCache.get(key);

        if (!bucket.tryConsume(1)) {
            writeTooManyRequests(response, request);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String buildClientKey(HttpServletRequest request) {
        String clientIp = extractClientIp(request);
        return "rl:" + clientIp + "|" + request.getMethod() + "|" + request.getRequestURI();
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(
            HttpServletResponse response,
            HttpServletRequest request
    ) throws IOException {
        String correlationId = request.getHeader(CorrelationIdFilter.HEADER);

        GatewayErrorBody body = GatewayErrorBody.of(
                GatewayErrorCode.RATE_LIMIT_EXCEEDED,
                GatewayErrorCode.RATE_LIMIT_EXCEEDED.name(),
                request.getRequestURI(),
                correlationId
        );

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
