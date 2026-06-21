package com.stop.match_service.common.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MatchRatingErrorCode implements ErrorCode {
    RATING_CLOSED("MATCH_RATING_CLOSED", "Match rating feature has closed", HttpStatus.BAD_REQUEST),
    SELF_RATING_NOT_ALLOWED("SELF_RATING_NOT_ALLOWED", "A user cannot rate themselves", HttpStatus.BAD_REQUEST),
    MATCH_NOT_COMPLETED("MATCH_NOT_COMPLETED", "Match is not completed", HttpStatus.BAD_REQUEST),

    ;

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}