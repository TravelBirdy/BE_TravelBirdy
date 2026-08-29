package com.travelbird.global.error;

import java.util.Map;

public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> details;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.defaultMessage(), null);
    }

    public BusinessException(
        ErrorCode errorCode,
        String message,
        Map<String, Object> details
    ) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public Map<String, Object> details() {
        return details;
    }
}
