package com.travelbird.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // 인증/로그인 (3.1)
    INVALID_AUTH_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_AUTH_REQUEST"),
    KAKAO_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "KAKAO_AUTHENTICATION_FAILED"),
    KAKAO_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "KAKAO_SERVICE_UNAVAILABLE"),
    USER_NOT_ACTIVE(HttpStatus.FORBIDDEN, "USER_NOT_ACTIVE"),
    ACTIVE_SESSION_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 활성 로그인 세션이 존재합니다."),
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

    // Region / Place / 저장 장소
    REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 시군구 코드입니다."),
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 장소입니다."),
    SAVED_PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "저장하지 않은 장소입니다."),
    SAVED_PLACE_MEMO_TOO_LONG(HttpStatus.BAD_REQUEST, "메모는 최대 100자까지 입력할 수 있습니다."),
    INVALID_CURSOR(HttpStatus.BAD_REQUEST, "유효하지 않은 커서입니다. 첫 페이지부터 다시 조회해주세요."),

    // 게시글 (Part3, PR#12)
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."),
    POST_ACCESS_DENIED(HttpStatus.FORBIDDEN, "작성자만 수정·삭제할 수 있습니다."),
    POST_ALREADY_EXISTS_FOR_TRIP(HttpStatus.CONFLICT, "이미 해당 여행에 연결된 게시글이 있습니다."),
    INVALID_POST_CONTENT(HttpStatus.BAD_REQUEST, "발행하려면 제목과 본문이 필요합니다."),
    POST_TITLE_TOO_LONG(HttpStatus.BAD_REQUEST, "제목은 최대 50자까지 입력할 수 있습니다."),
    POST_CONTENT_TOO_LONG(HttpStatus.BAD_REQUEST, "본문은 최대 2,000자까지 입력할 수 있습니다."),
    POST_HASHTAG_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "해시태그는 최대 5개까지 등록할 수 있습니다."),
    POST_HASHTAG_TOO_LONG(HttpStatus.BAD_REQUEST, "해시태그는 각각 최대 10자까지 입력할 수 있습니다."),
    POST_IMAGE_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "이미지는 최대 10장까지 첨부할 수 있습니다."),
    POST_MODIFICATION_CONFLICT(HttpStatus.CONFLICT, "다른 요청에 의해 게시글이 먼저 수정되었습니다."),
    POST_ROUTE_LOCKED_AFTER_PUBLISH(HttpStatus.CONFLICT, "발행된 게시글의 경로 관련 항목은 변경할 수 없습니다."),
    POST_PLACE_NOT_IN_TRIP(HttpStatus.BAD_REQUEST, "선택한 장소가 해당 여행 일정에 포함되어 있지 않습니다."),

    // Trip (Part2) — TRIP_ACCESS_DENIED는 Part2가 이미 정의해서 Post 쪽 중복 정의를 없애고 그대로 재사용한다.
    INVALID_COMPANION_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 동행 유형입니다."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "조회 날짜 범위가 올바르지 않습니다."),
    INVALID_PLACE_ORDER_REQUEST(HttpStatus.BAD_REQUEST, "장소 순서 변경 요청이 올바르지 않습니다."),
    INVALID_TRIP_PACE(HttpStatus.BAD_REQUEST, "유효하지 않은 일정 밀도입니다."),
    INVALID_TRIP_PERIOD(HttpStatus.BAD_REQUEST, "여행 기간이 올바르지 않습니다."),
    PLACE_ALREADY_ADDED(HttpStatus.CONFLICT, "이미 이 여행에 추가된 장소입니다."),
    PLACE_ALREADY_ASSIGNED_TO_DAY(HttpStatus.CONFLICT, "이미 일정 Day에 배치된 장소입니다."),
    REGION_REQUIRED(HttpStatus.BAD_REQUEST, "여행 지역이 필요합니다."),
    THEME_REQUIRED(HttpStatus.BAD_REQUEST, "여행 테마를 하나 이상 선택해야 합니다."),
    TOO_MANY_THEMES(HttpStatus.BAD_REQUEST, "여행 테마는 최대 3개까지 선택할 수 있습니다."),
    TRIP_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 여행 일정에 접근할 권한이 없습니다."),
    TRIP_CANCELLED_READ_ONLY(HttpStatus.CONFLICT, "취소된 여행은 읽기 전용입니다."),
    TRIP_CANCEL_REQUIRES_POST_DELETION(HttpStatus.CONFLICT, "여행을 취소하려면 연결된 발행 게시글을 먼저 삭제해야 합니다."),
    TRIP_DATE_REQUIRED(HttpStatus.BAD_REQUEST, "여행 날짜가 필요합니다."),
    TRIP_DAY_NOT_FOUND(HttpStatus.NOT_FOUND, "여행 Day를 찾을 수 없습니다."),
    TRIP_DAY_PLACE_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "한 Day의 장소 수는 최대 15개입니다."),
    TRIP_DAY_REMOVAL_CONFIRMATION_REQUIRED(HttpStatus.CONFLICT, "장소가 포함된 Day 삭제에 사용자 확인이 필요합니다."),
    TRIP_MODIFICATION_CONFLICT(HttpStatus.CONFLICT, "여행 일정 수정 버전이 충돌했습니다."),
    TRIP_NOT_FOUND(HttpStatus.NOT_FOUND, "여행 일정을 찾을 수 없습니다."),
    TRIP_PLACE_DAY_MISMATCH(HttpStatus.BAD_REQUEST, "해당 일정 장소가 요청한 Day에 속하지 않습니다."),
    TRIP_PLACE_IMAGE_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "일정 장소 이미지 개수 제한을 초과했습니다."),
    TRIP_PLACE_MEMO_TOO_LONG(HttpStatus.BAD_REQUEST, "일정 장소 메모가 최대 길이를 초과했습니다."),
    TRIP_ROUTE_LOCKED_BY_PUBLISHED_POST(HttpStatus.CONFLICT, "발행된 게시글로 인해 여행 경로가 잠겨 있습니다."),

    // AI (Part2)
    AI_DAILY_REQUEST_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "AI 일정 추천 일일 요청 한도를 초과했습니다."),
    AI_JOB_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 AI 추천 작업에 접근할 권한이 없습니다."),
    AI_JOB_ID_CONFLICT(HttpStatus.CONFLICT, "동일 jobId에 다른 요청 내용이 전달되었습니다."),
    AI_JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "AI 추천 작업을 찾을 수 없습니다."),
    AI_PREVIEW_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 AI 일정 미리보기에 접근할 권한이 없습니다."),
    AI_PREVIEW_DAY_PLACE_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "AI 미리보기의 한 Day 장소 수가 15개를 초과했습니다."),
    AI_PREVIEW_EXPIRED(HttpStatus.GONE, "AI 일정 미리보기의 유효기간이 만료되었습니다."),
    AI_PREVIEW_MODIFICATION_CONFLICT(HttpStatus.CONFLICT, "AI 미리보기 수정 버전이 충돌했습니다."),
    AI_PREVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "AI 일정 미리보기를 찾을 수 없습니다."),
    AI_PREVIEW_PLACE_DUPLICATED(HttpStatus.CONFLICT, "AI 미리보기 전체에 같은 장소가 중복 배치되었습니다."),
    AI_ROUTE_REQUIRED_PLACE_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "필수 배치 장소 수가 여행 전체 수용량을 초과했습니다."),
    AI_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI 추천 서비스를 사용할 수 없습니다."),
    CONFLICTING_PLACE_POLICY(HttpStatus.UNPROCESSABLE_ENTITY, "장소 추천 정책 조건이 서로 충돌합니다."),
    EMPTY_WISHLIST(HttpStatus.BAD_REQUEST, "위시리스트가 비어 있습니다."),
    INCOMPATIBLE_PLACE_REGIONS(HttpStatus.UNPROCESSABLE_ENTITY, "선택한 장소의 지역이 추천 요청 지역과 일치하지 않습니다."),
    INSUFFICIENT_SAVED_PLACES(HttpStatus.UNPROCESSABLE_ENTITY, "저장 장소 기반 추천에 필요한 장소가 부족합니다."),
    INVALID_AI_PREVIEW_ROUTE(HttpStatus.BAD_REQUEST, "AI 미리보기 경로 구성이 올바르지 않습니다."),
    INVALID_AI_RESPONSE(HttpStatus.UNPROCESSABLE_ENTITY, "AI 추천 결과가 Backend 검증 규칙을 충족하지 않습니다."),
    INVALID_INTERNAL_AI_KEY(HttpStatus.UNAUTHORIZED, "Internal AI 인증 키가 유효하지 않습니다."),
    PLACE_REGION_MISMATCH(HttpStatus.UNPROCESSABLE_ENTITY, "장소가 요청한 지역과 일치하지 않습니다."),
    SAVED_PLACE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "선택한 장소는 사용자의 저장 장소가 아닙니다."),

    // 커뮤니티 (Part3, Phase4)
    SEARCH_QUERY_TOO_LONG(HttpStatus.BAD_REQUEST, "검색어는 최대 50자까지 입력할 수 있습니다."),
    SEARCH_QUERY_REQUIRED(HttpStatus.BAD_REQUEST, "검색어를 입력해주세요."),
    INVALID_SHARE_CHANNEL(HttpStatus.BAD_REQUEST, "유효하지 않은 공유 채널입니다."),
    REPORT_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "이미 신고한 게시글입니다."),

    // 경로 저장 (Part3, Phase5) — AI_PREVIEW_* 3개는 위 AI(Part2) 섹션 기존 코드를 그대로 재사용한다.
    CANNOT_SAVE_OWN_ROUTE(HttpStatus.BAD_REQUEST, "자신의 게시글은 저장할 수 없습니다."),

    // 공통
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 형식 또는 필드가 올바르지 않습니다."),
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

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
