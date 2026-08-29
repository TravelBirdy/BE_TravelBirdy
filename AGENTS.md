# TravelBird Backend — AGENTS.md

> 목적: TravelBird Backend를 Codex로 개발할 때 확정된 계약을 보존하고, 임의 설계·계약 이탈·불필요한 Skill 생성을 방지한다.
> 이 파일은 Backend Repository의 루트에 둔다.

---

## 1. Project Scope

이 Repository의 Backend 개발은 아래 4개 기준 파일과 이 문서에서 확정한 Backend 기술 규칙만을 기준으로 진행한다.

### Authoritative Source Documents

```text
docs/source/
├─ backend-functional-spec-v10.md
├─ travelbird-openapi-v10.json
├─ ai-backend-integration-v10.md
└─ frontend-api-ux-guide-v10.md
```

각 파일의 역할은 다음과 같다.

1. `backend-functional-spec-v10.md`
   - Backend 비즈니스 규칙
   - 권한
   - 상태 전이
   - 도메인 정책
   - MVP 포함/제외
   - DB 구현 요구사항

2. `travelbird-openapi-v10.json`
   - HTTP Method
   - Path
   - Request / Response Schema
   - required / nullable
   - Enum
   - HTTP Status
   - ErrorResponse
   - Internal API 기계 계약

3. `ai-backend-integration-v10.md`
   - Spring Boot가 책임지는 AI Job 연동
   - place sync
   - recommendation request
   - callback
   - timeout
   - idempotency
   - Backend 결과 검증

4. `frontend-api-ux-guide-v10.md`
   - Backend 공개 API의 UX 처리 의미
   - required / nullable
   - PATCH
   - Cursor Pagination
   - 204
   - 상태 코드 처리
   - Trip 편집 가능 상태
   - AI Job polling / Preview 처리

---

## 2. Source of Truth Priority

계약 충돌 시 다음 우선순위를 적용한다.

### 2.1 API 기계 계약
`travelbird-openapi-v10.json`

다음은 OpenAPI가 최종 기준이다.

- Method
- Path
- Request Schema
- Response Schema
- required
- nullable
- Enum
- HTTP Status
- ErrorResponse 구조

### 2.2 비즈니스 정책
`backend-functional-spec-v10.md`

다음은 기능명세가 최종 기준이다.

- 권한
- 소유권
- 상태 전이
- 공개 범위
- 데이터 처리 정책
- Trip/Post/Place/AI 도메인 규칙
- MVP 범위

### 2.3 AI 연동 의미
`ai-backend-integration-v10.md`

Spring Boot가 수행해야 하는 AI 통신 의미와 운영 규칙은 이 문서를 OpenAPI와 함께 확인한다.

### 2.4 Frontend 처리 의미
`frontend-api-ux-guide-v10.md`

Frontend가 Backend 응답을 어떤 의미로 사용하는지는 이 문서를 참고한다.

---

## 3. Authoritative Source Protection

`docs/source/` 아래 4개 파일은 ordinary implementation 작업 중 임의 수정하지 않는다.

구현 중 계약 변경이 필요하면 다음 순서를 따른다.

1. 현재 Source와 구현의 충돌 지점을 명확히 보고한다.
2. 실제 설계 결정이 필요한 경우 `Superpowers: Brainstorming`을 우선 사용한다.
3. 사용자 승인 없이 계약을 변경하지 않는다.
4. 승인 후 authoritative source를 먼저 수정한다.
5. 그 다음 DBML / Harness 문서 / 구현을 갱신한다.
6. 테스트 후 변경 결과를 검증한다.

다음 행동은 금지한다.

- 현재 코드를 맞추기 위해 OpenAPI를 몰래 변경
- 구현 편의를 위해 Business Rule 축소
- OpenAPI에 없는 Field 추가
- 기능명세에 없는 상태값 추가
- 문서에 없는 Enum literal 추측
- 미확정 DB 정책을 임의 확정

---

## 4. Confirmed Backend Stack

다음 기술 스택을 Backend 표준으로 사용한다.

- Java 21 LTS
- Spring Boot 3.4.x
- Spring Security 6.4.x
- Gradle
- MySQL 8.0+
- Spring Data JPA
- Hibernate
- Lombok
- MapStruct
- QueryDSL
- Flyway
- JUnit 5
- Spring Boot Test
- Testcontainers MySQL
- JWT Access / Refresh Token

### Version Rule

Spring Boot `3.4.x`의 정확한 patch version은 프로젝트 초기화 시 하나를 고정한다.

금지:

```text
3.4.+
latest.release
+
```

동적 버전은 사용하지 않는다.

---

## 5. Superpowers Policy

### 5.1 Existing Superpowers First

일반적인 작업을 위해 Project 전용 Skill을 새로 만들지 않는다.

Codex 환경에 Superpowers가 제공되는 경우 다음과 같이 사용한다.

- 신규 기능
- 정책 변경
- 기존 계약 충돌
- 복수 설계안 비교
- 요구사항이 모호한 경우

위 상황에서는 `Superpowers: Brainstorming`을 우선 사용한다.

기존 계약에 이미 정의된 기능 구현에는 불필요하게 Brainstorming부터 수행하지 않는다.

### 5.2 Custom Skill Creation Restriction

현재 Repository에는 불필요한 `.agents/skills/*`를 만들지 않는다.

새 Skill은 다음 조건을 모두 만족할 때만 추가한다.

1. TravelBird에만 존재하는 반복 절차다.
2. `AGENTS.md + docs`만으로 안정적으로 처리하기 어렵다.
3. 기존 Superpowers와 기능이 중복되지 않는다.
4. 최소 2회 이상 반복될 것이 명확하다.

---

## 6. Mandatory Architecture Rules

### 6.1 Responsibility

```text
Controller
    ↓
Service
    ↓
Repository
```

- Controller는 HTTP 입출력과 Validation 연결에 집중한다.
- Business Logic은 Service에 둔다.
- Repository에 Business Policy를 숨기지 않는다.
- Service가 Transaction과 ownership을 관리한다.

### 6.2 Domain-first Package Structure

기본 구조:

```text
com.travelbird
├─ global
│  ├─ config
│  ├─ security
│  ├─ error
│  └─ common
├─ auth
├─ user
├─ onboarding
├─ file
├─ place
├─ trip
├─ post
├─ community
├─ savedroute
├─ social
├─ event
├─ home
├─ search
└─ ai
```

각 Domain:

```text
<domain>/
├─ controller
├─ dto/
│  ├─ request
│  └─ response
├─ service
├─ entity
├─ repository/
│  └─ querydsl
└─ mapper
```

실제 Gradle group이 `com.travelbird`와 다르게 확정되어 있다면 base package 문자열만 변경하고 domain-first 구조는 유지한다.

---

## 7. DTO Rules

### 7.1 Java record

불변 Request / Response DTO는 Java `record`를 적극 사용한다.

예:

```java
public record RegionSummary(
    String sigunguCode,
    String sigunguName
) {}
```

Bean Validation도 record component에 선언한다.

```java
public record CreateTripRequest(
    @NotBlank String regionCode,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @NotNull CompanionType companionType,
    @NotEmpty @Size(max = 3) Set<TravelTheme> themes,
    @NotNull Pace pace
) {}
```

### 7.2 PATCH Exception

PATCH에서 다음을 구분해야 한다.

- Field 미전달
- explicit null
- value 전달

이 구분 때문에 mutable class 또는 별도 wrapper가 실제로 필요한 경우 class DTO를 사용할 수 있다.

`Optional<T>`만 사용했다고 해서 미전달과 explicit null이 자동으로 해결된다고 가정하지 않는다.

### 7.3 Entity exposure

API Response에서 JPA Entity 직접 반환 금지.

항상 Response DTO를 사용한다.

---

## 8. Lombok Rules

Entity에서 허용:

```text
@Getter
@NoArgsConstructor(access = PROTECTED)
```

필요할 경우 제한적으로 Builder 사용 가능.

금지:

```text
@Data
전체 @Setter
LAZY association이 포함된 @ToString
LAZY association이 포함된 @EqualsAndHashCode
```

Entity 상태 변경은 의미 있는 method로 수행한다.

예:

```java
public void updateMemo(String memo) {
    this.memo = memo;
}
```

---

## 9. MapStruct Rules

MapStruct 사용 권장:

- Entity → Response DTO
- 단순 nested mapping
- 반복되는 mapping

MapStruct에서 수행하지 않는다.

- 권한 검증
- DB 조회
- Business Rule
- 상태 전이
- PATCH semantics
- 소유권 확인

권장 compiler option:

```text
unmappedTargetPolicy = ERROR
```

Mapping 누락을 compile 단계에서 발견한다.

---

## 10. QueryDSL Rules

QueryDSL 사용:

- Cursor Pagination
- Community 복합 조회
- Post 검색/필터
- Visibility + Status + Block 조합
- Trip 날짜 조건
- Popular ranking
- Photomap
- 통계성 조회

QueryDSL을 사용하지 않아도 되는 경우:

```text
findById
단순 FK 조회
단순 Unique 조회
```

Q-type generated source는 Git에 커밋하지 않는다.

---

## 11. JPA Rules

- Association은 LAZY 기본
- N:M 직접 매핑 금지
- 연결 Entity 사용
- Collection EAGER 금지
- Entity API 직접 반환 금지
- Service ownership 검증 필수

낙관적 락 대상:

- Trip
- Post
- AiTripPreview

DB Unique 제약과 Service 사전 검증을 함께 사용한다.

예상 가능한 `DataIntegrityViolationException`은 generic 500으로 끝내지 말고 해당 Business ErrorCode로 변환한다.

---

## 12. Authentication / Authorization

### JWT

JWT Payload:

- userId
- role
- tokenType
- iat
- exp

Access Token:

- 30분
- Stateless

Refresh Token:

- 30일
- MySQL 저장
- 원문 저장 금지
- hash만 저장
- 사용자당 활성 Refresh Token 하나

### Ownership

사용자 소유 데이터는 JWT userId를 기준으로 검증한다.

Request Body의:

```text
userId
ownerId
```

등을 권한 근거로 신뢰하지 않는다.

Spring Security URL authorization만으로 종료하지 않는다.

Service에서 최종 Resource ownership을 다시 검증한다.

---

## 13. Database Contract

Database Source:

```text
docs/database/travelbird.dbml
```

DB Schema 변경은:

```text
DBML
→ Flyway Migration
→ Entity
→ Testcontainers MySQL
```

순서로 진행한다.

### Mandatory Constraints

반드시 유지:

- `(provider, external_place_id)` Unique
- `(user_id, place_id)` saved place Unique
- `(trip_id, place_id)` Trip 전체 place Unique
- `(trip_id, place_id)` Trip wishlist Unique
- `(preview_id, place_id)` AI Preview 전체 place Unique
- Day별 order Unique
- `(reporter_user_id, post_id)` Unique
- `(user_id, source_type, source_id)` SavedRoute Unique
- Follow pair Unique
- Block pair Unique

### Place Canonical ID

```text
places.place_id
```

가 서비스 내부 canonical ID다.

외부 ID는:

```text
place_external_ids
```

에서 관리한다.

Provider:

```text
KTO_TOUR_API
NAVER
```

TourAPI contentId를 `places.place_id`로 직접 사용하지 않는다.

---

## 14. Flyway Rules

Schema 변경은 Flyway로만 적용한다.

경로:

```text
src/main/resources/db/migration/
```

예:

```text
V1__init_schema.sql
V2__add_xxx.sql
```

적용된 migration 파일은 수정하지 않는다.

JPA:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

금지:

```text
create
create-drop
update
```

DBML 변경 시 Flyway migration도 함께 변경한다.

---

## 15. Testcontainers Rules

DB Integration Test는 MySQL Testcontainers를 사용한다.

H2를 최종 DB 호환성 검증용으로 사용하지 않는다.

최소 검증:

- Flyway startup
- FK
- NOT NULL
- `(provider, external_place_id)` Unique
- Trip 전체 place Unique
- Preview 전체 place Unique
- Saved Place Unique
- Wishlist Unique
- Report Unique
- SavedRoute Unique
- Cursor ordering
- 낙관적 락

---

## 16. API Contract Rules

### Success Status

- 일반 성공 + Body → `200`
- 생성 + Body → `201`
- 비동기 접수 → `202`
- 성공 + Body 없음 → `204`

204에서 Response JSON을 생성하지 않는다.

### required / nullable

required:

> Key가 반드시 존재한다.

nullable:

> Key의 값이 null일 수 있다.

required + nullable이면 값이 null이어도 Key를 생략하지 않는다.

Array는 계약상 required라면 결과가 없어도:

```json
[]
```

을 반환한다.

### PATCH

- 미전달 → 유지
- explicit null → nullable 값 제거
- `[]` → 배열 비우기
- non-nullable explicit null → 400

### Cursor Pagination

Naver 외부 장소 검색을 제외한 내부 목록:

- cursor 생략 → 첫 페이지
- size 기본 20
- size 최대 50
- items required + non-null
- 결과 없음 → `[]`
- nextCursor required + nullable

다음은 추가하지 않는다.

```text
page
totalPage
pageNumber
```

---

## 17. Error Contract

공통 Response:

```json
{
  "code": "...",
  "message": "...",
  "timestamp": "...",
  "details": {}
}
```

규칙:

- code: required
- message: required
- timestamp: required
- details: optional + nullable

Client는 `message` 문자열이 아니라 `code`를 기준으로 분기한다.

Business/API 오류는 `ErrorCode` enum으로 관리한다.

AI Job 상태의 실패 진단 코드는 `AiJobFailureCode`로 분리한다.

### Place external mapping conflict

이미 같은 canonical placeId에 연결된 동일 mapping:

```text
멱등 성공
```

이미 다른 canonical placeId에 연결된 동일:

```text
(provider, external_place_id)
```

mapping 충돌:

```text
409 PLACE_EXTERNAL_ID_MAPPING_CONFLICT
```

---

## 18. CORS / Spring Security 6.4

CORS는 `CorsConfigurationSource` Bean을 직접 생성한다.

SecurityFilterChain:

```java
.cors(cors ->
    cors.configurationSource(corsConfigurationSource)
)
```

사용.

### Allowed Origins

환경변수:

```text
CORS_ALLOWED_ORIGINS
```

사용.

`allowCredentials=true`이므로 wildcard `*`를 사용하지 않는다.

### Allowed Methods

```text
GET
POST
PUT
DELETE
PATCH
OPTIONS
```

### Allowed Headers

Browser public API:

```text
Authorization
Content-Type
Accept
Origin
```

Internal AI 인증 Header:

```text
X-Internal-AI-Key
```

는 Browser CORS 허용 Header에 추가하지 않는다.

### Exposed Headers

현재 명시적으로 필요한 Header만 노출한다.

기본:

```text
Location
```

### Credentials

```text
true
```

### Max Age

```text
3600 seconds
```

### Preflight

```text
OPTIONS /**
```

는 JWT 없이 허용한다.

---

## 19. Environment / Secret Rules

실제 Secret은 코드/yml/Git에 직접 기록하지 않는다.

Git 공유 파일:

```text
.env.example
```

로컬 실제 값:

```text
.env
```

`.env`는 Git에서 제외한다.

### Environment Keys

최소:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
NAVER_CLIENT_ID
NAVER_CLIENT_SECRET
AI_SERVER_BASE_URL
TRAVELBIRD_AI_CALLBACK_KEY
CORS_ALLOWED_ORIGINS
AWS_REGION
S3_BUCKET
```

`application.yml`은 다음과 같이 환경변수를 참조한다.

```yaml
password: ${DB_PASSWORD}
secret: ${JWT_SECRET}
client-secret: ${NAVER_CLIENT_SECRET}
internal-key: ${TRAVELBIRD_AI_CALLBACK_KEY}
```

실제 값을 yml에 직접 적지 않는다.

---

## 20. Logging Security

로그에 기록하지 않는다.

- JWT Raw Token
- Refresh Token Raw Value
- Kakao Access Token
- DB Password
- Naver Secret
- Internal AI Key
- Presigned URL의 전체 Signature Query

로그 가능:

- userId
- tripId
- postId
- placeId
- jobId
- ErrorCode
- correlation/request id

---

## 21. Existing Feature Development Workflow

이미 v10에 존재하는 기능은 새 PRD/API 설계 문서를 만들지 않는다.

순서:

1. OpenAPI Operation 확인
2. Backend 기능명세 해당 절 확인
3. 관련 Harness rule 확인
4. DBML 확인
5. 현재 코드 확인
6. 구현 gap 분석
7. 테스트 시나리오 작성
8. 최소 범위 구현
9. Testcontainers 포함 테스트
10. OpenAPI ↔ Service ↔ DBML 교차 검증
11. `docs/harness/08-current-status.md` 갱신

---

## 22. New Feature / Contract Change Workflow

기존 4개 기준 파일에 없는 기능 또는 기존 정책 변경:

1. 현재 계약과 충돌 분석
2. `Superpowers: Brainstorming`
3. 가능한 선택지 정리
4. 사용자 결정
5. authoritative source 먼저 수정
6. OpenAPI / DBML / Flyway 반영
7. 구현
8. 테스트
9. Status 문서 갱신

---

## 23. Definition of Done

작업 완료 전에 반드시 확인한다.

### API
- Method/Path 일치
- Request 일치
- Response 일치
- required/nullable 일치
- HTTP Status 일치
- ErrorCode 일치

### Security
- JWT 인증
- Service ownership
- PRIVATE/BLOCKED 존재 숨김 정책
- Secret 하드코딩 없음

### Database
- DBML 반영
- Flyway migration 반영
- NOT NULL 검증
- Unique 검증
- Index 검토
- MySQL Testcontainers 성공

### Code
- Entity 직접 반환 없음
- immutable DTO record 사용 검토
- Entity @Data / 전체 @Setter 없음
- MapStruct가 Business Logic을 포함하지 않음
- QueryDSL 과사용 없음

### Test
- `./gradlew test` 성공
- Flyway startup 성공
- MySQL Testcontainers 성공

### Documentation
- 미확정 사항을 임의로 구현하지 않았는가?
- 계약 변경 시 Source를 먼저 수정했는가?
- `08-current-status.md`가 최신인가?

---

## Daily Log

- 의미 있는 작업 종료 시 `daily-log/YYYY.MM.DD.md`를 생성 또는 갱신한다.
- 완료/미완료 작업, Error와 확인된 원인, 문제사항, 결정사항, 변경 파일, 실제 Test 결과, 실제 Git Commit hash/message, 현재 상태와 다음 작업을 기록한다.
- 같은 날짜의 기존 기록을 보존한다.
- 실행하지 않은 Test 또는 존재하지 않는 Commit을 기록하지 않는다.
- Secret 값은 Daily Log에 기록하지 않는다.
- Daily Log가 구현 계획을 대신하지 않으며, 사용자가 요청하지 않은 전체 구현 계획을 임의 생성하지 않는다.

---
## 24. Prohibited Actions

다음은 금지한다.

- 계약에 없는 API 추가
- 계약에 없는 Response Field 추가
- DB Schema를 상식으로 임의 확장
- UserStatus 등 미확정 Enum literal 임의 추가
- Internal API를 Browser 공개 API로 변경
- `ddl-auto=update`
- Entity 직접 반환
- Request userId 기반 authorization
- 모든 기능마다 PRD 재작성
- 모든 작업마다 새로운 Skill 생성
- 기존 Superpowers와 중복되는 Skill 생성
- 테스트하지 않고 완료 선언
- Secret/API Key/Password 커밋
