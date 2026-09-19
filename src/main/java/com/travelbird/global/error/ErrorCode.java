package com.travelbird.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // 인증/로그인 (3.1)
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

    // 성향테스트/온보딩 (3.3, 3.4)
    PERSONALITY_TEST_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "PERSONALITY_TEST_UNAVAILABLE"),
    INCOMPLETE_PERSONALITY_TEST(HttpStatus.BAD_REQUEST, "모든 질문에 답변해야 합니다."),
    DUPLICATED_PERSONALITY_ANSWER(HttpStatus.BAD_REQUEST, "하나의 질문에는 하나의 답변만 가능합니다."),
    INVALID_PERSONALITY_OPTION(HttpStatus.BAD_REQUEST, "유효하지 않은 질문 또는 선택지입니다."),
    PERSONALITY_TEST_VERSION_MISMATCH(HttpStatus.CONFLICT, "최신 성향 테스트 버전이 아닙니다. 문항을 다시 조회해 주세요."),
    PERSONALITY_TEST_ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 완료된 성향 테스트입니다."),
    TIE_BREAKER_NOT_REQUIRED(HttpStatus.CONFLICT, "Tie-breaker가 필요하지 않은 제출입니다."),
    INVALID_TIE_BREAKER_SELECTION(HttpStatus.BAD_REQUEST, "INVALID_TIE_BREAKER_SELECTION"),
    PERSONALITY_SUBMISSION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "PERSONALITY_SUBMISSION_ACCESS_DENIED"),

    // 프로필/소셜 (3.2, 3.13)
    INVALID_PROFILE_VALUE(HttpStatus.BAD_REQUEST, "INVALID_PROFILE_VALUE"),
    CANNOT_FOLLOW_SELF(HttpStatus.BAD_REQUEST, "CANNOT_FOLLOW_SELF"),
    CANNOT_FOLLOW_BLOCKED_USER(HttpStatus.FORBIDDEN, "CANNOT_FOLLOW_BLOCKED_USER"),
    CANNOT_BLOCK_SELF(HttpStatus.BAD_REQUEST, "CANNOT_BLOCK_SELF"),

    // 파일 업로드 (3.5)
    UNSUPPORTED_FILE_TYPE(HttpStatus.BAD_REQUEST, "UNSUPPORTED_FILE_TYPE"),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE"),
    IMAGE_DIMENSION_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "IMAGE_DIMENSION_LIMIT_EXCEEDED"),
    IMAGE_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "IMAGE_LIMIT_EXCEEDED"),
    FILE_METADATA_MISMATCH(HttpStatus.CONFLICT, "FILE_METADATA_MISMATCH"),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND"),
    FILE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "FILE_ACCESS_DENIED"),

    // 축제·이벤트 (3.17) — 기능 보류 중, 코드만 미리 등록
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND"),

    // Region 도메인 (Part3, PR#6)
    REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 시군구 코드입니다."),

    // Place 도메인 (Part3, PR#7)
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 장소입니다."),

    // 저장 장소 (Part3, PR#9)
    SAVED_PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "저장하지 않은 장소입니다."),
    SAVED_PLACE_MEMO_TOO_LONG(HttpStatus.BAD_REQUEST, "메모는 최대 100자까지 입력할 수 있습니다."),
    INVALID_CURSOR(HttpStatus.BAD_REQUEST, "유효하지 않은 커서입니다. 첫 페이지부터 다시 조회해주세요."),

    // 게시글 (Part3, PR#12)
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
    POST_ROUTE_LOCKED_AFTER_PUBLISH(HttpStatus.CONFLICT, "발행된 게시글의 경로 관련 항목은 변경할 수 없습니다."),

    // 공통
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "잘못된 요청입니다.");

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
