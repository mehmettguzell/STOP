package com.stop.match_service.common.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum InvitationErrorCode implements ErrorCode {
    INVALID_INVITATION(
            "INV_000",
            "Self-invitation is not allowed. A user cannot invite themselves to a match.",
            HttpStatus.BAD_REQUEST
    ),

    PENDING_INVITATION_EXISTS(
            "INV_001",
            "There is already a pending invitation for this user in this match.",
            HttpStatus.CONFLICT
    ),

    INVITATION_ALREADY_PROCESSED(
            "INV_002",
            "This invitation has already been accepted or rejected.",
            HttpStatus.BAD_REQUEST
    ),

    INVITATION_NOT_FOUND(
            "INV_003",
            "The invitation could not be found.",
            HttpStatus.NOT_FOUND
    ),

    INVITATION_EXPIRED(
            "INV_004",
            "The invitation has expired.",
            HttpStatus.GONE
    );
    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
