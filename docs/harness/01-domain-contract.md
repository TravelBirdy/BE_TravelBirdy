# 01. Backend Domain Contract

## 공통

- API Prefix: `/api/`
- 서버 내부 시간 저장: UTC
- Client 시간 표현: ISO 8601
- 날짜 상태 판정 / 일일 제한: `Asia/Seoul`
- MySQL charset: `utf8mb4`
- ID: Java `Long` / OpenAPI `int64` / JSON number
- 내부 목록: Cursor Pagination, size 기본 20 / 최대 50
- PATCH:
  - 미전달 -> 기존값 유지
  - explicit null -> nullable 필드 삭제
  - `[]` -> 배열 비우기
  - non-nullable explicit null -> 400

## User/Auth

- `users.role`: ROLE_USER | ROLE_ADMIN
- 신규 사용자 기본 ROLE_USER
- Refresh Token은 MySQL에 hash만 저장
- 사용자당 활성 Refresh Token 하나
- Access Token 30분
- Refresh Token 30일
- Access Token blacklist는 MVP 제외

## Region

- 내부 지역 식별자는 5자리 시군구 String
- `sigungu_master`가 기준정보
- Trip 생성/AI 추천은 단일 시군구
- 복수 지역은 검색/필터에서만 사용

## Place

- canonical ID: `places.place_id`
- external ID: `place_external_ids`
- Provider: KTO_TOUR_API | NAVER
- Unique: `(provider, external_place_id)`
- TourAPI contentId를 placeId로 사용하지 않음
- NAVER 공개 resolve 요청에는 provider를 받지 않음
- 자동 canonical 병합:
  1. 정규화 이름 일치
  2. 정규화 주소 일치
  3. 좌표 50m 이내
  4. 고신뢰 후보 정확히 1개
- 후보 없음/복수/불명확 -> 새 canonical place 생성

## Trip

- sourceType: MANUAL | AI
- 기본 visibility: PRIVATE
- 상태:
  - UPCOMING / IN_PROGRESS / COMPLETED는 날짜로 계산
  - cancelledAt 존재 -> CANCELLED 우선
- CompanionType: SOLO | COUPLE | FRIENDS | FAMILY | OTHER
- TravelTheme: ACTIVITY | SNS_HOTPLACE | NATURE | ATTRACTION | SHOPPING | FOOD
- Theme 1~3개, 중복 금지
- Pace: RELAXED | NORMAL | DENSE
- Pace는 하루 2/3/4곳 고정 의미가 아님
- Day별 장소 최대 15
- 하나의 Trip 전체에서 동일 placeId 최대 1회
- cancelled Trip은 모든 수정 차단
- 발행 Post가 삭제되지 않은 동안 Trip route lock 유지
- Trip 공개 범위와 Post 공개 범위는 독립

## Post

- 하나의 Trip에는 삭제되지 않은 Post 최대 1개
- DRAFT는 route lock을 만들지 않음
- 최초 publish 시 route lock
- status: DRAFT | PUBLISHED | BLOCKED
- visibility: PUBLIC | MEMO_PRIVATE | PRIVATE
- title <= 50
- content <= 2000
- hashtag <= 5개, 각 <= 10
- image <= 10
- publish=false: title/content null 허용
- publish=true: title/content 필수
- 삭제 시 deletedAt 기록, 사용자 콘텐츠 제거
- 조회수/저장수/공유수 유지

## Saved Place / Wishlist

- saved place Unique(userId, placeId)
- saved place memo <= 100
- wishlist Unique(tripId, placeId)
- Day에 배치된 placeId는 같은 Trip wishlist에 동시에 존재하지 않음

## Social

- Follow는 단방향
- Unique(followerUserId, followingUserId)
- Block Unique(blockerUserId, blockedUserId)
- 자기 자신 follow/block 금지
- block 시 양방향 follow 관계 제거

## AI Backend State

- Backend job: QUEUED | PROCESSING | SUCCEEDED | FAILED | EXPIRED
- Spring Boot가 Long jobId 생성
- Backend `PROCESSING` 전환 시점부터 Callback timeout 180초
- Callback 전체 검증과 Preview 저장 성공 후에만 SUCCEEDED
- Preview: TEMPORARY -> 기본 24시간
- 경로 저장 -> PERMANENT, expiresAt=null
- 마지막 AI SavedRoute 취소 -> TEMPORARY로 강등, 취소 시점+24시간
- Preview 전체에서 동일 placeId 최대 1회
- Preview Day 최대 15
- Preview 수정은 version 낙관적 락
- AI Preview 적용은 멱등
