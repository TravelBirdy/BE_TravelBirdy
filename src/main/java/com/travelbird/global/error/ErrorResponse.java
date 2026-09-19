package com.travelbird.global.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

public record ErrorResponse(
        String code,
        String message,
        OffsetDateTime timestamp,
        @JsonInclude(JsonInclude.Include.NON_NULL) Map<String, Object> details) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return of(errorCode, errorCode.defaultMessage(), null);
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return of(errorCode, message, null);
    }

    public static ErrorResponse of(ErrorCode errorCode, String message, Map<String, Object> details) {
        return new ErrorResponse(
                errorCode.name(),
                message == null || message.isBlank() ? errorCode.defaultMessage() : message,
                OffsetDateTime.now(ZoneOffset.UTC),
                details);
    }
}
