package com.travelbird.global.error;

import java.time.OffsetDateTime;

public record ErrorResponse(
        String code,
        String message,
        OffsetDateTime timestamp
) {

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.name(), message, OffsetDateTime.now());
    }
}
