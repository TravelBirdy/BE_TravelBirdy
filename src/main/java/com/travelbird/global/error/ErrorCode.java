package com.travelbird.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    INVALID_AUTH_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_AUTH_REQUEST"),
    KAKAO_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "KAKAO_AUTHENTICATION_FAILED"),
    KAKAO_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "KAKAO_SERVICE_UNAVAILABLE"),
    USER_NOT_ACTIVE(HttpStatus.FORBIDDEN, "USER_NOT_ACTIVE"),
    ACTIVE_SESSION_ALREADY_EXISTS(HttpStatus.CONFLICT, "ACTIVE_SESSION_ALREADY_EXISTS"),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "만료된 Refresh Token입니다."),
    REFRESH_TOKEN_REVOKED(HttpStatus.UNAUTHORIZED, "폐기된 Refresh Token입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다."),
    TOKEN_ACCESS_DENIED(HttpStatus.FORBIDDEN, "TOKEN_ACCESS_DENIED"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN"),
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 장소입니다."),
    SAVED_PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "저장하지 않은 장소입니다."),
    SAVED_PLACE_MEMO_TOO_LONG(HttpStatus.BAD_REQUEST, "메모는 최대 100자까지 입력할 수 있습니다."),
    INVALID_CURSOR(HttpStatus.BAD_REQUEST, "유효하지 않은 커서입니다. 첫 페이지부터 다시 조회해주세요.");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
