package com.stop.match_service.common.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MatchErrorCode implements ErrorCode {
    MATCH_NOT_FOUND("MATCH_NOT_FOUND", "Match not found", HttpStatus.NOT_FOUND),
    UNAUTHORIZED("UNAUTHORIZED_ACCESS", "You are not authorized to perform this action on this match", HttpStatus.FORBIDDEN),
    MATCH_NOT_EDITABLE("MATCH_NOT_EDITABLE", "Match cannot be edited or updated in its current state", HttpStatus.BAD_REQUEST),
    INVALID_STATUS_TRANSITION("INVALID_STATUS_TRANSITION", "Match cannot be transitioned to the requested status from its current state", HttpStatus.BAD_REQUEST),
    INVALID_MATCH_CAPACITY("INVALID_MATCH_CAPACITY", "Match capacity cannot be less than the current number of participants", HttpStatus.BAD_REQUEST),
    MATCH_NOT_AVAILABLE("MATCH_NOT_AVAILABLE", "Match is not available", HttpStatus.BAD_REQUEST);
    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
