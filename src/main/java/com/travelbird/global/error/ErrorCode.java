package com.travelbird.global.error;

import org.springframework.http.HttpStatus;

/**
 * Stable business/API error code catalog.
 *
 * The UI/client must branch on code, not on message.
 * PLACE_NOT_FOUND uses 404 in Spring Boot public/resource APIs.
 * Backend->AI remote 422 PLACE_NOT_FOUND is interpreted at the AI client boundary.
 */
public enum ErrorCode {

    ACTIVE_SESSION_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 활성 로그인 세션이 존재합니다."),
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
    CANNOT_BLOCK_SELF(HttpStatus.BAD_REQUEST, "자기 자신을 차단할 수 없습니다."),
    CANNOT_SAVE_OWN_ROUTE(HttpStatus.BAD_REQUEST, "자신의 게시글 경로는 저장할 수 없습니다."),
    CONFLICTING_PLACE_POLICY(HttpStatus.UNPROCESSABLE_ENTITY, "장소 추천 정책 조건이 서로 충돌합니다."),
    DUPLICATED_PERSONALITY_ANSWER(HttpStatus.BAD_REQUEST, "같은 질문에 중복 답변할 수 없습니다."),
    EMPTY_WISHLIST(HttpStatus.BAD_REQUEST, "Empty wishlist."),
    FILE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 파일에 접근할 권한이 없습니다."),
    FILE_METADATA_MISMATCH(HttpStatus.CONFLICT, "업로드된 파일 정보가 요청 메타데이터와 일치하지 않습니다."),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "파일 크기가 허용 범위를 초과했습니다."),
    IMAGE_DIMENSION_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "이미지 해상도 제한을 초과했습니다."),
    IMAGE_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "이미지 첨부 개수 제한을 초과했습니다."),
    INCOMPATIBLE_PLACE_REGIONS(HttpStatus.UNPROCESSABLE_ENTITY, "선택한 장소의 지역이 추천 요청 지역과 일치하지 않습니다."),
    INCOMPLETE_PERSONALITY_TEST(HttpStatus.BAD_REQUEST, "모든 성향 테스트 문항에 답변해야 합니다."),
    INSUFFICIENT_SAVED_PLACES(HttpStatus.UNPROCESSABLE_ENTITY, "저장 장소 기반 추천에 필요한 장소가 부족합니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    INVALID_AI_PREVIEW_ROUTE(HttpStatus.BAD_REQUEST, "AI 미리보기 경로 구성이 올바르지 않습니다."),
    INVALID_AI_RESPONSE(HttpStatus.UNPROCESSABLE_ENTITY, "AI 추천 결과가 Backend 검증 규칙을 충족하지 않습니다."),
    INVALID_AUTH_REQUEST(HttpStatus.BAD_REQUEST, "인증 요청 형식이 올바르지 않습니다."),
    INVALID_COMPANION_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 동행 유형입니다."),
    INVALID_COORDINATES(HttpStatus.BAD_REQUEST, "유효하지 않은 좌표입니다."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "조회 날짜 범위가 올바르지 않습니다."),
    INVALID_INTERNAL_AI_KEY(HttpStatus.UNAUTHORIZED, "Internal AI 인증 키가 유효하지 않습니다."),
    INVALID_PERSONALITY_OPTION(HttpStatus.BAD_REQUEST, "유효하지 않은 질문 또는 선택지입니다."),
    INVALID_PLACE_CATEGORY(HttpStatus.BAD_REQUEST, "유효하지 않은 장소 카테고리입니다."),
    INVALID_PLACE_ORDER_REQUEST(HttpStatus.BAD_REQUEST, "장소 순서 변경 요청이 올바르지 않습니다."),
    INVALID_POST_CONTENT(HttpStatus.BAD_REQUEST, "발행에 필요한 게시글 제목 또는 본문이 없습니다."),
    INVALID_PROFILE_VALUE(HttpStatus.BAD_REQUEST, "프로필 값이 허용 범위를 벗어났습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 형식 또는 필드가 올바르지 않습니다."),
    INVALID_SHARE_CHANNEL(HttpStatus.BAD_REQUEST, "유효하지 않은 공유 채널입니다."),
    INVALID_TIE_BREAKER_SELECTION(HttpStatus.BAD_REQUEST, "유효하지 않은 Tie-breaker 선택입니다."),
    INVALID_TRIP_PACE(HttpStatus.BAD_REQUEST, "유효하지 않은 일정 밀도입니다."),
    INVALID_TRIP_PERIOD(HttpStatus.BAD_REQUEST, "여행 기간이 올바르지 않습니다."),
    KAKAO_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "카카오 인증에 실패했습니다."),
    KAKAO_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "카카오 인증 서비스를 사용할 수 없습니다."),
    PERSONALITY_SUBMISSION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 성향 테스트 제출에 접근할 권한이 없습니다."),
    PERSONALITY_TEST_ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 완료된 성향 테스트입니다."),
    PERSONALITY_TEST_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "현재 사용할 수 있는 성향 테스트가 없습니다."),
    PERSONALITY_TEST_VERSION_MISMATCH(HttpStatus.CONFLICT, "최신 성향 테스트 버전이 아닙니다."),
    PLACE_ALREADY_ADDED(HttpStatus.CONFLICT, "이미 이 여행에 추가된 장소입니다."),
    PLACE_ALREADY_ASSIGNED_TO_DAY(HttpStatus.CONFLICT, "이미 일정 Day에 배치된 장소입니다."),
    PLACE_EXTERNAL_ID_MAPPING_CONFLICT(HttpStatus.CONFLICT, "외부 장소 식별자가 다른 canonical 장소에 이미 연결되어 있습니다."),
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "장소를 찾을 수 없습니다."),
    PLACE_REGION_MISMATCH(HttpStatus.UNPROCESSABLE_ENTITY, "장소가 요청한 지역과 일치하지 않습니다."),
    PLACE_SEARCH_DAILY_LIMIT_EXCEEDED(HttpStatus.SERVICE_UNAVAILABLE, "금일 장소 검색 한도가 초과되었습니다."),
    PLACE_SEARCH_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "장소 검색 외부 서비스를 사용할 수 없습니다."),
    POST_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 게시글에 접근할 권한이 없습니다."),
    POST_ALREADY_EXISTS_FOR_TRIP(HttpStatus.CONFLICT, "해당 여행에는 이미 삭제되지 않은 게시글이 존재합니다."),
    POST_CONTENT_TOO_LONG(HttpStatus.BAD_REQUEST, "게시글 본문이 최대 길이를 초과했습니다."),
    POST_HASHTAG_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "게시글 해시태그 개수 제한을 초과했습니다."),
    POST_HASHTAG_TOO_LONG(HttpStatus.BAD_REQUEST, "게시글 해시태그가 최대 길이를 초과했습니다."),
    POST_IMAGE_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "게시글 이미지 개수 제한을 초과했습니다."),
    POST_MODIFICATION_CONFLICT(HttpStatus.CONFLICT, "게시글 수정 버전이 충돌했습니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),
    POST_ROUTE_LOCKED_AFTER_PUBLISH(HttpStatus.CONFLICT, "발행된 게시글의 경로 정보는 변경할 수 없습니다."),
    POST_TITLE_TOO_LONG(HttpStatus.BAD_REQUEST, "게시글 제목이 최대 길이를 초과했습니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Refresh Token이 만료되었습니다."),
    REFRESH_TOKEN_REVOKED(HttpStatus.UNAUTHORIZED, "Refresh Token이 폐기되었습니다."),
    REGION_REQUIRED(HttpStatus.BAD_REQUEST, "여행 지역이 필요합니다."),
    REPORT_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "이미 신고한 게시글입니다."),
    SAVED_PLACE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "선택한 장소는 사용자의 저장 장소가 아닙니다."),
    SAVED_PLACE_MEMO_TOO_LONG(HttpStatus.BAD_REQUEST, "저장 장소 메모가 최대 길이를 초과했습니다."),
    SEARCH_QUERY_REQUIRED(HttpStatus.BAD_REQUEST, "검색어가 필요합니다."),
    SEARCH_QUERY_TOO_LONG(HttpStatus.BAD_REQUEST, "검색어가 너무 깁니다."),
    SEARCH_QUERY_TOO_SHORT(HttpStatus.BAD_REQUEST, "검색어가 너무 짧습니다."),
    THEME_REQUIRED(HttpStatus.BAD_REQUEST, "여행 테마를 하나 이상 선택해야 합니다."),
    TIE_BREAKER_NOT_REQUIRED(HttpStatus.CONFLICT, "Tie-breaker가 필요하지 않은 제출입니다."),
    TOKEN_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 토큰에 접근할 권한이 없습니다."),
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
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    UNSUPPORTED_FILE_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다."),
    USER_NOT_ACTIVE(HttpStatus.FORBIDDEN, "활성 상태의 사용자가 아닙니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
