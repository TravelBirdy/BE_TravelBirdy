# 트래블버드 백엔드 개발 스펙 (전체)

- 작성 기준 문서: 트래블버드 통합 백엔드 기능명세서 결정사항 반영본 v5 (기준일 2026-08-06)
- 작성일: 2026-08-06
- 대상 독자: 백엔드 개발팀 (Spring Boot 구현 담당) / 프론트엔드(React Native) 참고용
- 범위: 서비스 전체 도메인 (3.1 ~ 3.19)
- 비고: 이 문서는 **개발 착수 전 전체 설계 가이드**이며, API 상세 요청/응답 스펙(Swagger)은 별도 문서에서 다룬다.

---

## 1. 서비스 한눈에 보기

트래블버드는 국내 여행 일정 계획·기록·공유 서비스다. 사용자는 직접 또는 AI 도움을 받아 일정을 짜고, 여행 중 사진/메모를 남기고, 여행 후 기록(게시글)을 발행해 커뮤니티에 공유한다.

**MVP에서 제외되는 기능** (개발 시 헷갈리지 않도록 명시)
- 관리자 웹 페이지 / 관리자 전용 API
- 캐릭터 경험치·레벨·성장 단계
- 리워드 포인트 (적립/사용/거래내역)
- Redis (캐시/블랙리스트 모두 미사용, DB 또는 애플리케이션 단기 캐시로 대체)

## 2. 기술 아키텍처 요약

| 항목 | 내용 |
|---|---|
| 모바일 클라이언트 | React Native |
| 백엔드 프레임워크 | Spring Boot + Spring Security |
| 데이터베이스 | MySQL (utf8mb4) |
| 인증 | JWT Access Token + Refresh Token |
| 외부 지도/장소 검색 | 네이버 API |
| 외부 축제 데이터 | 한국관광공사 TourAPI |
| 인프라 | AWS EC2, RDS(MySQL), S3 |
| 서비스 구조 | 초기: 모듈형 모놀리스 (하나의 Spring Boot 앱, 도메인별 패키지 분리) |
| 요청 흐름 | React Native → HTTPS/ALB → Spring Boot → RDS·S3·네이버 API·AI 서버 |
| 이미지 저장 | 원본은 프론트에서 WebP로 최적화 후 S3 업로드, DB에는 메타데이터+Object Key만 저장 |
| 시크릿 관리 | AWS Secrets Manager 또는 Parameter Store (JWT Secret, 네이버 API Key, DB 비밀번호) |
| AI 연동 방식 | 비동기 HTTP + Callback (동기 대기 없음, jobId 폴링) |

## 3. 전체 의존성 (build.gradle)

```gradle
dependencies {
    // === 기본 웹 ===
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    // === 인증/보안 ===
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'io.jsonwebtoken:jjwt-api:0.12.6'          // JWT 생성/검증
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'

    // === DB / ORM ===
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    runtimeOnly 'com.mysql:mysql-connector-j'
    // 커서 페이지네이션, 복잡 조회 조건이 많아 QueryDSL 도입 검토 권장
    implementation 'com.querydsl:querydsl-jpa:5.1.0:jakarta'
    annotationProcessor 'com.querydsl:querydsl-apt:5.1.0:jakarta'

    // === 외부 API 연동 (카카오, 네이버, AI 서버, TourAPI 호출용) ===
    implementation 'org.springframework.boot:spring-boot-starter-webflux' // WebClient

    // === AWS 연동 (S3 업로드 URL 발급, Secrets Manager) ===
    implementation platform('software.amazon.awssdk:bom:2.28.0')
    implementation 'software.amazon.awssdk:s3'
    implementation 'software.amazon.awssdk:secretsmanager'

    // === 스케줄러 (배치 작업: 만료 정리, 축제 데이터 동기화 등) ===
    // Spring Boot 기본 @Scheduled 사용 (별도 라이브러리 불필요)
    // 작업량이 늘어나면 Quartz 도입 검토: implementation 'org.springframework.boot:spring-boot-starter-quartz'

    // === 개발 편의 ===
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    developmentOnly 'org.springframework.boot:spring-boot-devtools'

    // === API 문서화 ===
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0'

    // === 테스트 ===
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
}
```

> 라이브러리 버전은 실제 프로젝트 생성 시점 기준 최신 안정 버전으로 재확인 필요.

## 4. 전역 공통 원칙 (모든 도메인 공통 적용)

- API 기본 Prefix: `/api/`
- 서버 내부 시간: UTC 저장 → 클라이언트 응답 시 ISO 8601 형식으로 변환
- 날짜 기준 상태 판정/일일 제한: `Asia/Seoul` 기준 날짜 사용 (예: AI 추천 하루 3회 제한, 조회수 24시간 제한 등)
- JPA Entity는 기본 지연 로딩(LAZY), N:1이 아닌 N 관계는 연결 Entity로 분리
- API 응답은 Entity를 직접 반환하지 않고 반드시 **Request/Response DTO** 사용
- 모든 사용자 소유 데이터는 **JWT의 사용자 ID 기준으로 권한 검증** (Request Body의 사용자 ID는 신뢰하지 않음)
- 로그인 필수 API에 인증정보 없으면 `401 UNAUTHORIZED`
- 비로그인 접근 허용 API는 "선택적 인증" 적용 (로그인 시 개인화된 정보 추가 제공)
- Spring Security의 URL 인가만으로 끝내지 않고, **Service 계층에서 리소스 소유권 재검증** (특히 여행 일정, 게시글 등)

## 5. 패키지 구조 제안 (모듈형 모놀리스, 도메인 기준)

```
com.travelbird
 ├─ common          (공통 응답, 예외 처리, JWT 필터, 보안 설정)
 ├─ auth            (3.1 카카오 로그인, 토큰 재발급, 로그아웃, 회원탈퇴)
 ├─ onboarding      (3.3 성향테스트, 3.4 파트너새)
 ├─ file            (3.5 이미지 업로드 Presigned URL)
 ├─ place           (3.6 장소 검색/상세/저장)
 ├─ trip            (3.7 여행 일정, Day, 위시리스트, 취소)
 ├─ post            (3.8 여행 기록/게시글)
 ├─ community        (3.9 커뮤니티 목록/검색/조회수/공유/신고)
 ├─ savedroute       (3.10 경로 저장)
 ├─ photomap         (3.11 포토맵)
 ├─ home            (3.12 홈)
 ├─ social           (3.13 팔로우/차단)
 ├─ mypage           (3.14 마이페이지)
 ├─ search           (3.16 통합 검색)
 ├─ event            (3.17 축제/이벤트)
 ├─ map              (3.18 지도 경로 표시)
 └─ ai              (3.19 AI 여행 추천, Callback)
```

## 6. 도메인별 핵심 DB 테이블 (초안)

> 컬럼 타입은 기능명세서 기준으로 정리한 **초안**이며, 실제 Entity 설계 시 담당 개발자가 세부 조정 필요.

### 6.1 인증/회원 (3.1)
- `users` (id, email?, nickname?, introduction?, bird_type?, role, status, onboarding_completed, created_at, updated_at)
- `user_social_accounts` (id, user_id FK, provider, provider_user_id, created_at) — `(provider, provider_user_id)` UNIQUE
- `refresh_tokens` (id, user_id FK, token_hash, expires_at, revoked_at?, created_at)

### 6.2 온보딩/파트너새 (3.3, 3.4)
- `personality_test_versions` (test_version, is_active)
- `personality_questions` (id, test_version, question_order, text)
- `personality_options` (id, question_id FK, trait_code, text) — trait_code: GOURMET/REST/PHOTO/ACTIVITY/CULTURE
- `personality_submissions` (id, user_id FK, test_version, result_status, tied_traits?, created_at)
- `personality_answers` (id, submission_id FK, question_id, option_id)
- `bird_trait_mapping` **[추가 확정 필요]** — 5개 성향 코드와 5종 파트너 새(오목눈이·물총새·호반새·딱새·동박새) 매핑표 원본 데이터 없음, 별도 확인 필요

### 6.3 이미지 업로드 (3.5)
- `files` (id, owner_user_id, status: PENDING/UPLOADED/LINKED, object_key, content_type, size_bytes, width, height, purpose: POST/TRIP_PLACE, created_at)

### 6.4 장소 (3.6)
- `places` (id, provider, external_place_id, name, external_category, category(정규화 Enum), address, region_code?, latitude, longitude, status: ACTIVE/CLOSED) — `(provider, external_place_id)` UNIQUE
- `place_category_mapping_rules` (id, priority, keyword, target_category, is_excluded)
- `saved_places` (id, user_id FK, place_id FK, memo?, saved_at) — `(user_id, place_id)` UNIQUE

### 6.5 여행 일정 (3.7)
- `trips` (id, owner_user_id, title, summary?, region_code, start_date, end_date, companion_type, pace, visibility, source_type: MANUAL/AI, cancelled_at?, version, created_at, updated_at)
- `trip_days` (id, trip_id FK, day_number)
- `trip_places` (id, trip_day_id FK, place_id FK, order, planned_time?, duration_minutes?, memo?)
- `trip_place_images` (id, trip_place_id FK, file_id FK)
- `trip_wishlist_places` (id, trip_id FK, place_id FK) — `(trip_id, place_id)` UNIQUE
- `trip_themes` (trip_id FK, theme) — N:M 연결 테이블

### 6.6 여행 기록 (3.8)
- `posts` (id, trip_id FK, title, content, representative_file_id?, status: DRAFT/PUBLISHED/BLOCKED, visibility, published_at?, deleted_at?, view_count, save_count, share_count, version, created_at)
- `post_images` (id, post_id FK, file_id FK, order)
- `post_places` (id, post_id FK, place_id FK) — 포토맵/통계 집계용
- `post_hashtags` (id, post_id FK, tag)
- `post_view_histories` (id, post_id FK, viewer_user_id, viewed_at) — 24시간 중복 방지용
- `post_daily_metrics` (id, post_id FK, date, view_count, save_count, share_count) — 인기 점수 집계 배치용

### 6.7 커뮤니티/신고 (3.9)
- `reports` (id, reporter_user_id FK, post_id FK, reason_code, description?, created_at) — `(reporter_user_id, post_id)` UNIQUE

### 6.8 경로 저장 (3.10)
- `saved_routes` (id, user_id FK, source_type: POST/AI_PREVIEW, source_id, source_available, saved_at, updated_at)

### 6.9 포토맵 (3.11)
- 별도 테이블 없이 `posts` + `post_places` 기준 집계 쿼리로 처리 (성능 이슈 시 지역별 집계 테이블 분리 검토)

### 6.10 홈 (3.12)
- `home_recommended_places` (id, place_id FK, display_order) — 운영자가 직접 지정
- (축제 데이터는 6.13 `events` 재사용)

### 6.11 이웃새/차단 (3.13)
- `follows` (id, follower_user_id FK, following_user_id FK) — `(follower_user_id, following_user_id)` UNIQUE
- `user_blocks` (id, blocker_user_id FK, blocked_user_id FK) — `(blocker_user_id, blocked_user_id)` UNIQUE

### 6.12 마이페이지 (3.14)
- 별도 테이블 없음, `posts`/`trips`/`follows`/`post_places` 집계 쿼리

### 6.13 축제/이벤트 (3.17)
- `events` (id, name, region, place_name, start_date, end_date, thumbnail_url, source: TOUR_API) — TourAPI에서 매일 새벽 배치 동기화

### 6.14 AI 여행 추천 (3.19)
- `ai_jobs` (id, user_id FK, request_type: GENERAL/SAVED_PLACES/TRIP_WISHLIST, status: QUEUED/PROCESSING/SUCCEEDED/FAILED/EXPIRED, requested_at, started_at?, completed_at?, error_code?)
- `ai_previews` (id, job_id FK, user_id FK, trip_title, summary, hashtags, retention_status: TEMPORARY/PERMANENT, expires_at?, saved_at?, version, created_at, updated_at)
- `ai_preview_days` (id, preview_id FK, day_number)
- `ai_preview_places` (id, preview_day_id FK, place_id FK, order, reason, recommended_time?, duration_minutes?)

## 7. 인증/인가 정책 요약 (전 도메인 공통)

- JWT 인증은 `OncePerRequestFilter` 상속 필터에서 처리, `Authorization: Bearer {accessToken}` 헤더 검증
- Access Token 30분 / Refresh Token 1개월, 사용자당 활성 Refresh Token 1개만 허용
- Refresh Token은 MySQL에 해시로 저장 (Redis 미사용)
- 인증 오류는 `AuthenticationEntryPoint`, 인가 오류는 `AccessDeniedHandler`에서 공통 JSON 포맷으로 응답
- `users.role`: `ROLE_USER`(기본) / `ROLE_ADMIN`(DB에서 수동 지정, 별도 관리자 API 없음)

## 8. 외부 연동 정리

| 연동 대상 | 용도 | 비고 |
|---|---|---|
| 카카오 API | 로그인 (사용자 정보 조회) | SDK 발급 Access Token 검증 방식만 사용 |
| 네이버 API | 국내 장소 검색 | 일일 무료 호출 한도 초과 시 당일 검색 차단 (과금 전환 없음) |
| 한국관광공사 TourAPI | 축제/이벤트 데이터 | 매일 새벽 1회 배치 수집 |
| 자체 AI 서버 | 일정 추천 | 비동기 HTTP 요청 + Callback (`POST /internal/ai-callbacks/trip-recommendations`) |
| AWS S3 | 이미지 저장 | Presigned URL 방식, 원본 서버 미경유 |

## 9. 비동기 처리 아키텍처 (AI 추천 - 3.19)

1. 클라이언트가 `POST /api/ai/trip-recommendations` 호출 → 백엔드는 `ai_jobs`를 `QUEUED`로 생성하고 즉시 202 + jobId 응답
2. 백엔드 내부 비동기 디스패처가 AI 서버에 요청 전달 후 연결 종료 (결과 대기 안 함)
3. AI 서버가 접수하면 `PROCESSING`으로 변경
4. AI 서버 처리 완료 후 내부 Callback API로 결과 전달 → 백엔드가 검증 후 `ai_previews` 생성, job을 `SUCCEEDED`/`FAILED`로 변경
5. 클라이언트는 `GET /api/ai/trip-recommendations/{jobId}`로 폴링하여 상태 확인
6. 내부 Callback API는 외부 사용자 API와 분리하고 내부 인증키/서명 검증 적용 필요

> 구현 시 `@Async` + `TaskExecutor` 조합 또는 별도 메시지 큐(SQS 등) 도입 여부는 트래픽 규모에 따라 추후 결정.

## 10. 배치/스케줄러 목록

| 작업 | 주기 | 설명 |
|---|---|---|
| 미완료 이미지 파일 정리 | 매일 | `files` 중 24시간 동안 `PENDING`/`UPLOADED`인 채로 미연결된 항목 삭제 |
| AI 미리보기 만료 처리 | 주기적 (예: 1시간마다) | `TEMPORARY` 상태이고 `expires_at` 지난 `ai_previews` 정리 |
| 축제 데이터 동기화 | 매일 새벽 1회 | TourAPI → `events` 테이블 갱신 |
| 인기 게시글 점수 집계 | 배치 또는 실시간 집계 쿼리 | `post_daily_metrics` 기준, Redis 미사용이므로 DB 집계 |

## 11. 도메인별 API 개수 요약 (참고용, 상세 스펙은 별도 문서)

| 섹션 | 도메인 | 주요 API 수 |
|---|---|---|
| 3.1 | 인증/회원 | 4 |
| 3.2 | 프로필 | 2 |
| 3.3~3.4 | 온보딩/파트너새 | 3 |
| 3.5 | 이미지 업로드 | 2 |
| 3.6 | 장소 | 3 |
| 3.7 | 여행 일정 | 9 |
| 3.8 | 여행 기록 | 4 |
| 3.9 | 커뮤니티 | 6 |
| 3.10 | 경로 저장 | 6 |
| 3.11 | 포토맵 | 2 |
| 3.12 | 홈 | 1 |
| 3.13 | 이웃새/차단 | 6 |
| 3.14 | 마이페이지 | 1 |
| 3.16 | 통합 검색 | 1 |
| 3.17 | 축제/이벤트 | 2 |
| 3.18 | 지도 경로 표시 | 1 |
| 3.19 | AI 여행 추천 | 7 |

> 총 약 60개 내외 API. 담당자별로 섹션을 나눠 API 명세서(Swagger)를 작성하는 방식 권장.

## 12. 공통 에러 응답 포맷

```json
{
  "code": "ERROR_CODE_STRING",
  "message": "사람이 읽을 수 있는 에러 메시지",
  "timestamp": "2026-08-06T12:00:00Z"
}
```

- 400: 잘못된 요청 (필수값 누락, 형식 오류)
- 401: 인증 필요/실패
- 403: 권한 없음 (본인 리소스 아님)
- 404: 리소스 없음 (비공개 리소스도 존재 여부를 숨기기 위해 404로 통일하는 경우 다수)
- 409: 상태 충돌 (중복, 잠금, 이미 처리됨 등)
- 422: 도메인 규칙 위반 (개수 제한 초과 등)
- 429: 요청 횟수 제한 초과 (예: AI 추천 하루 3회)
- 503: 외부 서비스 장애

## 13. 전체 도메인 공통 - 추후 확정 필요 사항 모음

- 성향 코드(GOURMET/REST/PHOTO/ACTIVITY/CULTURE) ↔ 파트너 새 5종 매핑표
- 날씨 API 공급자 및 장애 시 대체 응답 정책 (홈 화면)
- 여행 취소 API(`POST /api/trips/{tripId}/cancel`)는 문서상 [AI 제안·구현 기준]으로 표시되어 있어 최종 확정 필요
- JWT Secret 등 시크릿 관리 방식 최종 확정 (Secrets Manager vs Parameter Store)
- MySQL에서 "삭제되지 않은 Post 1개만 Trip에 연결" 제약을 생성 컬럼+유니크 인덱스로 구현할지, 애플리케이션 레벨 검증으로 처리할지 결정 필요

---

## 부록. 참고 문서

- 원본: 트래블버드 통합 백엔드 기능명세서 결정사항 반영본 v5
- 다음 단계 산출물: 도메인별 API 명세서 (Swagger/OpenAPI YAML), ERD 다이어그램
