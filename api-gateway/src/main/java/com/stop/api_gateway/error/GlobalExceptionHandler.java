package com.stop.api_gateway.error;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Downstream servis kapalı / bağlantı reddedildi ─────────────────────────
    @ExceptionHandler({ResourceAccessException.class, ConnectException.class})
    public ResponseEntity<GatewayErrorBody> handleConnectionRefused(
            Exception ex,
            HttpServletRequest request
    ) {
        log.warn("Downstream servis ulaşılamaz: {} — {}", request.getRequestURI(), ex.getMessage());
        return serviceUnavailable(request, "Servis şu an ulaşılamıyor, lütfen daha sonra tekrar deneyin.");
    }

    // ── Timeout ─────────────────────────────────────────────────────────────────
    @ExceptionHandler({SocketTimeoutException.class, TimeoutException.class})
    public ResponseEntity<GatewayErrorBody> handleTimeout(
            Exception ex,
            HttpServletRequest request
    ) {
        log.warn("Downstream timeout: {} — {}", request.getRequestURI(), ex.getMessage());
        String correlationId = request.getHeader("X-Correlation-Id");
        GatewayErrorBody body = GatewayErrorBody.of(
                GatewayErrorCode.DOWNSTREAM_TIMEOUT,
                "İstek zaman aşımına uğradı.",
                request.getRequestURI(),
                correlationId
        );
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(body);
    }

    // ── Circuit breaker açık ────────────────────────────────────────────────────
    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<GatewayErrorBody> handleCircuitOpen(
            CallNotPermittedException ex,
            HttpServletRequest request
    ) {
        log.warn("Circuit breaker açık: {}", request.getRequestURI());
        return serviceUnavailable(request, "Servis geçici olarak devre dışı, kısa süre içinde tekrar deneyin.");
    }

    // ── Spring ResponseStatusException ──────────────────────────────────────────
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<GatewayErrorBody> handleResponseStatus(
            ResponseStatusException ex,
            HttpServletRequest request
    ) {
        // İçinde connection/timeout hatası saklı olabilir
        if (ex.getCause() instanceof ConnectException || ex.getCause() instanceof ResourceAccessException) {
            return handleConnectionRefused(ex, request);
        }
        if (ex.getCause() instanceof SocketTimeoutException || ex.getCause() instanceof TimeoutException) {
            return handleTimeout(ex, request);
        }
        if (ex.getCause() instanceof CallNotPermittedException cbnp) {
            return handleCircuitOpen(cbnp, request);
        }

        HttpStatusCode statusCode = ex.getStatusCode();
        GatewayErrorCode code = mapStatus(statusCode);
        String correlationId = request.getHeader("X-Correlation-Id");
        GatewayErrorBody body = GatewayErrorBody.of(
                code,
                ex.getReason() != null ? ex.getReason() : code.name(),
                request.getRequestURI(),
                correlationId
        );
        return ResponseEntity.status(statusCode).body(body);
    }

    // ── Beklenmedik tüm hatalar için güvenli fallback ───────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GatewayErrorBody> handleGeneric(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Gateway beklenmedik hata: {} — {}", request.getRequestURI(), ex.getMessage(), ex);
        String correlationId = request.getHeader("X-Correlation-Id");
        GatewayErrorBody body = GatewayErrorBody.of(
                GatewayErrorCode.INTERNAL_ERROR,
                "Beklenmedik bir hata oluştu.",
                request.getRequestURI(),
                correlationId
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    // ── Yardımcılar ─────────────────────────────────────────────────────────────
    private ResponseEntity<GatewayErrorBody> serviceUnavailable(HttpServletRequest request, String message) {
        String correlationId = request.getHeader("X-Correlation-Id");
        GatewayErrorBody body = GatewayErrorBody.of(
                GatewayErrorCode.SERVICE_UNAVAILABLE,
                message,
                request.getRequestURI(),
                correlationId
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    private static GatewayErrorCode mapStatus(HttpStatusCode status) {
        if (!(status instanceof HttpStatus hs)) return GatewayErrorCode.INTERNAL_ERROR;
        return switch (hs) {
            case NOT_FOUND -> GatewayErrorCode.ROUTE_NOT_DEFINED;
            case SERVICE_UNAVAILABLE -> GatewayErrorCode.SERVICE_UNAVAILABLE;
            case GATEWAY_TIMEOUT -> GatewayErrorCode.DOWNSTREAM_TIMEOUT;
            case TOO_MANY_REQUESTS -> GatewayErrorCode.RATE_LIMIT_EXCEEDED;
            case FORBIDDEN -> GatewayErrorCode.FORBIDDEN;
            default -> GatewayErrorCode.INTERNAL_ERROR;
        };
    }
}
