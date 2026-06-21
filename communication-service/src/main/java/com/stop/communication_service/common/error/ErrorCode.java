package com.stop.communication_service.common.error;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
    String getCode();
    String getMessage();
    HttpStatus getHttpStatus();
}
