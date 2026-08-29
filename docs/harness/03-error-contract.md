# 03. Error Contract

## 기본 원칙

- Client 분기는 `message`가 아니라 안정적인 `code`를 기준으로 한다.
- 공통 DTO: `ErrorResponse{code,message,timestamp,details}`
- `code`, `message`, `timestamp`: required
- `details`: optional + nullable
- 하나의 실제 오류 응답에는 하나의 `code`만 담는다.
- 예상 가능한 DB Unique 충돌은 Service에서 구체적인 ErrorCode로 변환한다.
- 비동기 AI Job의 `error.code`는 HTTP ErrorResponse와 별도 진단 값이므로 `AiJobFailureCode`로 분리한다.

## 장소 외부 ID 충돌 규칙 — 이번 Harness에서 명확화

- 같은 `(provider, external_place_id)`가 이미 **같은** canonical `place_id`에 연결됨: 오류 아님, 멱등 성공.
- 같은 `(provider, external_place_id)`가 이미 **다른** canonical `place_id`에 연결됨: `409 PLACE_EXTERNAL_ID_MAPPING_CONFLICT`.
- Naver 검색 외부 장애: 기존 계약 `503 PLACE_SEARCH_UNAVAILABLE`.
- AI 추천 전 장소 동기화 실패: HTTP 공개 오류가 아니라 AI Job 실패 진단 `AI_PLACE_SYNC_FAILED`.

## Spring Boot ErrorCode Catalog

| Code | HTTP | Default message |
|---|---:|---|
| `ACTIVE_SESSION_ALREADY_EXISTS` | 409 | 이미 활성 로그인 세션이 존재합니다. |
| `AI_DAILY_REQUEST_LIMIT_EXCEEDED` | 429 | AI 일정 추천 일일 요청 한도를 초과했습니다. |
| `AI_JOB_ACCESS_DENIED` | 403 | 해당 AI 추천 작업에 접근할 권한이 없습니다. |
| `AI_JOB_ID_CONFLICT` | 409 | 동일 jobId에 다른 요청 내용이 전달되었습니다. |
| `AI_JOB_NOT_FOUND` | 404 | AI 추천 작업을 찾을 수 없습니다. |
| `AI_PREVIEW_ACCESS_DENIED` | 403 | 해당 AI 일정 미리보기에 접근할 권한이 없습니다. |
| `AI_PREVIEW_DAY_PLACE_LIMIT_EXCEEDED` | 422 | AI 미리보기의 한 Day 장소 수가 15개를 초과했습니다. |
| `AI_PREVIEW_EXPIRED` | 410 | AI 일정 미리보기의 유효기간이 만료되었습니다. |
| `AI_PREVIEW_MODIFICATION_CONFLICT` | 409 | AI 미리보기 수정 버전이 충돌했습니다. |
| `AI_PREVIEW_NOT_FOUND` | 404 | AI 일정 미리보기를 찾을 수 없습니다. |
| `AI_PREVIEW_PLACE_DUPLICATED` | 409 | AI 미리보기 전체에 같은 장소가 중복 배치되었습니다. |
| `AI_ROUTE_REQUIRED_PLACE_LIMIT_EXCEEDED` | 422 | 필수 배치 장소 수가 여행 전체 수용량을 초과했습니다. |
| `AI_SERVICE_UNAVAILABLE` | 503 | AI 추천 서비스를 사용할 수 없습니다. |
| `CANNOT_BLOCK_SELF` | 400 | 자기 자신을 차단할 수 없습니다. |
| `CANNOT_SAVE_OWN_ROUTE` | 400 | 자신의 게시글 경로는 저장할 수 없습니다. |
| `CONFLICTING_PLACE_POLICY` | 422 | 장소 추천 정책 조건이 서로 충돌합니다. |
| `DUPLICATED_PERSONALITY_ANSWER` | 400 | 같은 질문에 중복 답변할 수 없습니다. |
| `EMPTY_WISHLIST` | 400 | Empty wishlist. |
| `FILE_ACCESS_DENIED` | 403 | 해당 파일에 접근할 권한이 없습니다. |
| `FILE_METADATA_MISMATCH` | 409 | 업로드된 파일 정보가 요청 메타데이터와 일치하지 않습니다. |
| `FILE_TOO_LARGE` | 413 | 파일 크기가 허용 범위를 초과했습니다. |
| `IMAGE_DIMENSION_LIMIT_EXCEEDED` | 422 | 이미지 해상도 제한을 초과했습니다. |
| `IMAGE_LIMIT_EXCEEDED` | 422 | 이미지 첨부 개수 제한을 초과했습니다. |
| `INCOMPATIBLE_PLACE_REGIONS` | 422 | 선택한 장소의 지역이 추천 요청 지역과 일치하지 않습니다. |
| `INCOMPLETE_PERSONALITY_TEST` | 400 | 모든 성향 테스트 문항에 답변해야 합니다. |
| `INSUFFICIENT_SAVED_PLACES` | 422 | 저장 장소 기반 추천에 필요한 장소가 부족합니다. |
| `INTERNAL_SERVER_ERROR` | 500 | 서버 내부 오류가 발생했습니다. |
| `INVALID_AI_PREVIEW_ROUTE` | 400 | AI 미리보기 경로 구성이 올바르지 않습니다. |
| `INVALID_AI_RESPONSE` | 422 | AI 추천 결과가 Backend 검증 규칙을 충족하지 않습니다. |
| `INVALID_AUTH_REQUEST` | 400 | 인증 요청 형식이 올바르지 않습니다. |
| `INVALID_COMPANION_TYPE` | 400 | 유효하지 않은 동행 유형입니다. |
| `INVALID_COORDINATES` | 400 | 유효하지 않은 좌표입니다. |
| `INVALID_DATE_RANGE` | 400 | 조회 날짜 범위가 올바르지 않습니다. |
| `INVALID_INTERNAL_AI_KEY` | 401 | Internal AI 인증 키가 유효하지 않습니다. |
| `INVALID_PERSONALITY_OPTION` | 400 | 유효하지 않은 질문 또는 선택지입니다. |
| `INVALID_PLACE_CATEGORY` | 400 | 유효하지 않은 장소 카테고리입니다. |
| `INVALID_PLACE_ORDER_REQUEST` | 400 | 장소 순서 변경 요청이 올바르지 않습니다. |
| `INVALID_POST_CONTENT` | 400 | 발행에 필요한 게시글 제목 또는 본문이 없습니다. |
| `INVALID_PROFILE_VALUE` | 400 | 프로필 값이 허용 범위를 벗어났습니다. |
| `INVALID_REFRESH_TOKEN` | 401 | 유효하지 않은 Refresh Token입니다. |
| `INVALID_REQUEST` | 400 | 요청 형식 또는 필드가 올바르지 않습니다. |
| `INVALID_SHARE_CHANNEL` | 400 | 유효하지 않은 공유 채널입니다. |
| `INVALID_TIE_BREAKER_SELECTION` | 400 | 유효하지 않은 Tie-breaker 선택입니다. |
| `INVALID_TRIP_PACE` | 400 | 유효하지 않은 일정 밀도입니다. |
| `INVALID_TRIP_PERIOD` | 400 | 여행 기간이 올바르지 않습니다. |
| `KAKAO_AUTHENTICATION_FAILED` | 401 | 카카오 인증에 실패했습니다. |
| `KAKAO_SERVICE_UNAVAILABLE` | 503 | 카카오 인증 서비스를 사용할 수 없습니다. |
| `PERSONALITY_SUBMISSION_ACCESS_DENIED` | 403 | 해당 성향 테스트 제출에 접근할 권한이 없습니다. |
| `PERSONALITY_TEST_ALREADY_COMPLETED` | 409 | 이미 완료된 성향 테스트입니다. |
| `PERSONALITY_TEST_UNAVAILABLE` | 503 | 현재 사용할 수 있는 성향 테스트가 없습니다. |
| `PERSONALITY_TEST_VERSION_MISMATCH` | 409 | 최신 성향 테스트 버전이 아닙니다. |
| `PLACE_ALREADY_ADDED` | 409 | 이미 이 여행에 추가된 장소입니다. |
| `PLACE_ALREADY_ASSIGNED_TO_DAY` | 409 | 이미 일정 Day에 배치된 장소입니다. |
| `PLACE_EXTERNAL_ID_MAPPING_CONFLICT` | 409 | 외부 장소 식별자가 다른 canonical 장소에 이미 연결되어 있습니다. |
| `PLACE_NOT_FOUND` | 404 | 장소를 찾을 수 없습니다. |
| `PLACE_REGION_MISMATCH` | 422 | 장소가 요청한 지역과 일치하지 않습니다. |
| `PLACE_SEARCH_DAILY_LIMIT_EXCEEDED` | 503 | 금일 장소 검색 한도가 초과되었습니다. |
| `PLACE_SEARCH_UNAVAILABLE` | 503 | 장소 검색 외부 서비스를 사용할 수 없습니다. |
| `POST_ACCESS_DENIED` | 403 | 해당 게시글에 접근할 권한이 없습니다. |
| `POST_ALREADY_EXISTS_FOR_TRIP` | 409 | 해당 여행에는 이미 삭제되지 않은 게시글이 존재합니다. |
| `POST_CONTENT_TOO_LONG` | 400 | 게시글 본문이 최대 길이를 초과했습니다. |
| `POST_HASHTAG_LIMIT_EXCEEDED` | 400 | 게시글 해시태그 개수 제한을 초과했습니다. |
| `POST_HASHTAG_TOO_LONG` | 400 | 게시글 해시태그가 최대 길이를 초과했습니다. |
| `POST_IMAGE_LIMIT_EXCEEDED` | 422 | 게시글 이미지 개수 제한을 초과했습니다. |
| `POST_MODIFICATION_CONFLICT` | 409 | 게시글 수정 버전이 충돌했습니다. |
| `POST_NOT_FOUND` | 404 | 게시글을 찾을 수 없습니다. |
| `POST_ROUTE_LOCKED_AFTER_PUBLISH` | 409 | 발행된 게시글의 경로 정보는 변경할 수 없습니다. |
| `POST_TITLE_TOO_LONG` | 400 | 게시글 제목이 최대 길이를 초과했습니다. |
| `REFRESH_TOKEN_EXPIRED` | 401 | Refresh Token이 만료되었습니다. |
| `REFRESH_TOKEN_REVOKED` | 401 | Refresh Token이 폐기되었습니다. |
| `REGION_REQUIRED` | 400 | 여행 지역이 필요합니다. |
| `REPORT_ALREADY_SUBMITTED` | 409 | 이미 신고한 게시글입니다. |
| `SAVED_PLACE_ACCESS_DENIED` | 403 | 선택한 장소는 사용자의 저장 장소가 아닙니다. |
| `SAVED_PLACE_MEMO_TOO_LONG` | 400 | 저장 장소 메모가 최대 길이를 초과했습니다. |
| `SEARCH_QUERY_REQUIRED` | 400 | 검색어가 필요합니다. |
| `SEARCH_QUERY_TOO_LONG` | 400 | 검색어가 너무 깁니다. |
| `SEARCH_QUERY_TOO_SHORT` | 400 | 검색어가 너무 짧습니다. |
| `THEME_REQUIRED` | 400 | 여행 테마를 하나 이상 선택해야 합니다. |
| `TIE_BREAKER_NOT_REQUIRED` | 409 | Tie-breaker가 필요하지 않은 제출입니다. |
| `TOKEN_ACCESS_DENIED` | 403 | 해당 토큰에 접근할 권한이 없습니다. |
| `TOO_MANY_THEMES` | 400 | 여행 테마는 최대 3개까지 선택할 수 있습니다. |
| `TRIP_ACCESS_DENIED` | 403 | 해당 여행 일정에 접근할 권한이 없습니다. |
| `TRIP_CANCELLED_READ_ONLY` | 409 | 취소된 여행은 읽기 전용입니다. |
| `TRIP_CANCEL_REQUIRES_POST_DELETION` | 409 | 여행을 취소하려면 연결된 발행 게시글을 먼저 삭제해야 합니다. |
| `TRIP_DATE_REQUIRED` | 400 | 여행 날짜가 필요합니다. |
| `TRIP_DAY_NOT_FOUND` | 404 | 여행 Day를 찾을 수 없습니다. |
| `TRIP_DAY_PLACE_LIMIT_EXCEEDED` | 422 | 한 Day의 장소 수는 최대 15개입니다. |
| `TRIP_DAY_REMOVAL_CONFIRMATION_REQUIRED` | 409 | 장소가 포함된 Day 삭제에 사용자 확인이 필요합니다. |
| `TRIP_MODIFICATION_CONFLICT` | 409 | 여행 일정 수정 버전이 충돌했습니다. |
| `TRIP_NOT_FOUND` | 404 | 여행 일정을 찾을 수 없습니다. |
| `TRIP_PLACE_DAY_MISMATCH` | 400 | 해당 일정 장소가 요청한 Day에 속하지 않습니다. |
| `TRIP_PLACE_IMAGE_LIMIT_EXCEEDED` | 422 | 일정 장소 이미지 개수 제한을 초과했습니다. |
| `TRIP_PLACE_MEMO_TOO_LONG` | 400 | 일정 장소 메모가 최대 길이를 초과했습니다. |
| `TRIP_ROUTE_LOCKED_BY_PUBLISHED_POST` | 409 | 발행된 게시글로 인해 여행 경로가 잠겨 있습니다. |
| `UNAUTHORIZED` | 401 | 인증이 필요합니다. |
| `UNSUPPORTED_FILE_TYPE` | 400 | 지원하지 않는 파일 형식입니다. |
| `USER_NOT_ACTIVE` | 403 | 활성 상태의 사용자가 아닙니다. |
| `USER_NOT_FOUND` | 404 | 사용자를 찾을 수 없습니다. |
