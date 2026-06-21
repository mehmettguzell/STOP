package com.stop.api_gateway.controller;

import com.stop.api_gateway.error.GatewayErrorBody;
import com.stop.api_gateway.error.GatewayErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Circuit breaker fallbackUri hedefi.
 * Downstream servis kapalıyken veya circuit açıkken çağrılır;
 * gateway 503 döner ama kendisi çalışmaya devam eder.
 */
@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/service-unavailable")
    public ResponseEntity<GatewayErrorBody> serviceUnavailable(HttpServletRequest request) {
        String originalPath = (String) request.getAttribute("jakarta.servlet.forward.request_uri");
        String correlationId = request.getHeader("X-Correlation-Id");
        log.warn("Fallback tetiklendi — orijinal istek: {}", originalPath);
        GatewayErrorBody body = GatewayErrorBody.of(
                GatewayErrorCode.SERVICE_UNAVAILABLE,
                "Bu servis şu an kullanılamıyor. Lütfen kısa süre sonra tekrar deneyin.",
                request.getRequestURI(),
                correlationId
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
