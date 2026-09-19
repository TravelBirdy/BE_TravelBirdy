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
    REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 시군구 코드입니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."),
    POST_ACCESS_DENIED(HttpStatus.FORBIDDEN, "작성자만 수정·삭제할 수 있습니다."),
    TRIP_ACCESS_DENIED(HttpStatus.FORBIDDEN, "본인 소유의 여행이 아닙니다."),
    POST_ALREADY_EXISTS_FOR_TRIP(HttpStatus.CONFLICT, "이미 해당 여행에 연결된 게시글이 있습니다."),
    INVALID_POST_CONTENT(HttpStatus.BAD_REQUEST, "발행하려면 제목과 본문이 필요합니다."),
    POST_TITLE_TOO_LONG(HttpStatus.BAD_REQUEST, "제목은 최대 50자까지 입력할 수 있습니다."),
    POST_CONTENT_TOO_LONG(HttpStatus.BAD_REQUEST, "본문은 최대 2,000자까지 입력할 수 있습니다."),
    POST_HASHTAG_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "해시태그는 최대 5개까지 등록할 수 있습니다."),
    POST_HASHTAG_TOO_LONG(HttpStatus.BAD_REQUEST, "해시태그는 각각 최대 10자까지 입력할 수 있습니다."),
    POST_IMAGE_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "이미지는 최대 10장까지 첨부할 수 있습니다."),
    POST_MODIFICATION_CONFLICT(HttpStatus.CONFLICT, "다른 요청에 의해 게시글이 먼저 수정되었습니다."),
    POST_ROUTE_LOCKED_AFTER_PUBLISH(HttpStatus.CONFLICT, "발행된 게시글의 경로 관련 항목은 변경할 수 없습니다.");

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
