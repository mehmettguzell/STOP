package com.stop.api_gateway.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GatewayErrorBody(
        GatewayErrorCode code,
        String message,
        String path,
        Instant timestamp,
        String correlationId
) {
    public static GatewayErrorBody of(
            GatewayErrorCode code,
            String message,
            String path,
            String correlationId
    ) {
        return new GatewayErrorBody(code, message, path, Instant.now(), correlationId);
    }
}
