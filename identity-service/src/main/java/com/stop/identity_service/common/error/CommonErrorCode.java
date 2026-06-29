package com.stop.identity_service.common.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    INVALID_REQUEST("INVALID_REQUEST", "Gecersiz istek", HttpStatus.BAD_REQUEST),

    UNAUTHORIZED("UNAUTHORIZED", "Oturum bilgisi eksik veya gecersiz", HttpStatus.UNAUTHORIZED),

    FORBIDDEN("FORBIDDEN", "Bu isleme yetkiniz yok", HttpStatus.FORBIDDEN),

    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "Aranan icerik bulunamadi", HttpStatus.NOT_FOUND),

    INTERNAL_ERROR("INTERNAL_ERROR", "Sunucu hatasi olustu. Lutfen daha sonra tekrar deneyin", HttpStatus.INTERNAL_SERVER_ERROR),

    SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE", "Servis su anda kullanilamiyor. Lutfen daha sonra tekrar deneyin", HttpStatus.SERVICE_UNAVAILABLE);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}