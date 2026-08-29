# TravelBird

## Frontend API & UX Integration Guide

### v10 최종 전달본

API 명세와 함께 프론트엔드가 반드시 공유해야 하는 UX/상태/에러/화면 처리 규칙

기준: `travelbird-openapi-v10.json` + `backend-functional-spec-v10.md`

2026-08-13

> **Repository Source 안내**
>
> Backend Repository에서는 이 Markdown 파일을 Frontend API/UX 기준 문서로 사용한다.
> 작업 기준은 이 Markdown 파일이다.
> API의 기계 판독 기준은 `travelbird-openapi-v10.json`,
> 비즈니스 정책 기준은 `backend-functional-spec-v10.md`를 우선한다.

---

## 0. 문서 사용법과 기준

### 이 문서의 역할

OpenAPI JSON은 정확한 Method/Path/Request/Response/required/nullable/HTTP Status 계약의 기준이다.  
이 가이드는 OpenAPI만으로 놓치기 쉬운 UX 흐름, 상태 전이, 화면 제어, 프론트 공통 구현 규칙을 설명한다.  
Figma는 화면 구성과 표현의 기준이지만 API 계약·권한·상태 규칙을 임의로 덮어쓰지 않는다.  
충돌이 발견되면 임의 추론하지 말고 최신 OpenAPI와 기능명세서를 기준으로 백엔드와 확인한다.

| 자료 | 프론트에서 보는 목적 | 우선 적용 |
|---|---|---|
| `travelbird-openapi-v10.json` | 필드 타입, required/nullable, Enum, Status, 오류 예시, Endpoint | API 계약의 1차 기준 |
| `backend-functional-spec-v10.md` | 비즈니스 정책, 상태 전이, 권한, UX 동작 | 행동/정책 기준 |
| 본 Frontend Guide | 화면 처리·공통 구현·연동 순서·주의사항 | 프론트 통합 구현 기준 |
| Figma | 화면 레이아웃·컴포넌트·사용자 동선 | 표현 기준 |

### 0.1 프론트가 직접 호출하면 안 되는 API

- `POST /internal/v1/recommendations` - Spring Boot → AI 서버 내부 호출 전용
- `PUT /internal/v1/places/sync` - Spring Boot → AI 런타임 장소 데이터 동기화 전용
- `POST /internal/ai-callbacks/trip-recommendations` - AI 서버 → Spring Boot Callback 전용
- 세 내부 API 모두 `X-Internal-AI-Key`를 사용하며 프론트엔드는 직접 호출하지 않는다.

> **핵심 원칙**  
> 프론트엔드는 AI 서버를 직접 호출하지 않는다. 모든 AI 요청·상태 조회·Preview 조회/수정/적용은 Spring Boot 공개 API만 사용한다.

---

## 1. 프론트엔드 공통 API 계약

### 1.1 인증과 토큰

- 카카오 로그인 입력은 React Native 카카오 SDK가 발급한 `kakaoAccessToken` 하나만 백엔드에 전달한다.
- 로그인 후 화면 분기는 `isNewUser`가 아니라 `onboardingCompleted`를 기준으로 한다. false면 온보딩, true면 메인으로 이동한다.
- `nickname`, `birdType`은 로그인 응답에서 Key가 항상 존재하지만 값은 null일 수 있다(required + nullable).
- Access Token TTL은 30분(1800초), Refresh Token TTL은 30일(2592000초)이며 두 `expiresIn` 값의 단위는 초(seconds)다.
- Refresh Token Rotation을 사용한다. 401이 동시에 여러 개 발생해도 refresh 요청은 한 번만 보내고 나머지 요청은 대기시킨 뒤 새 토큰 저장 후 재시도한다(Single-flight).
- 로그아웃은 Refresh Token을 폐기하지만 이미 발급된 Access Token은 최대 30분간 자연 만료 전까지 유효할 수 있다. 프론트는 로그아웃 즉시 로컬 토큰을 삭제한다.
- 활성 Refresh Token이 이미 있는 사용자의 신규 로그인은 `409 ACTIVE_SESSION_ALREADY_EXISTS`가 될 수 있다.

### 1.2 HTTP Status와 Body 처리

| Status | 프론트 처리 원칙 |
|---|---|
| 200 | Response Body가 있는 일반 성공. JSON을 파싱한다. |
| 201 | 리소스 생성 성공 + Body 반환. 생성된 ID를 응답에서 사용한다. |
| 202 | 비동기 작업 접수 또는 성향 테스트 동점 흐름. "완료"로 오해하지 않는다. |
| 204 | 성공 + Body 없음. `response.json()`을 호출하지 않는다. |
| 400 | 입력 형식/값 오류. ErrorResponse.code 기준으로 입력 UI를 보정한다. |
| 401 | 인증 필요/토큰 문제. Access 만료는 refresh Single-flight 처리 후 재시도한다. |
| 403 | 로그인했지만 권한 없음/소유권 위반. |
| 404 | 없음 또는 존재를 숨겨야 하는 PRIVATE/BLOCKED 리소스. 존재 여부를 별도로 노출하지 않는다. |
| 409 | 현재 상태와 충돌(중복, route lock, version conflict 등). 서버 상태를 다시 조회하는 흐름이 필요할 수 있다. |
| 410 | AI Preview 만료. 재추천 또는 다른 화면으로 안내한다. |
| 422 | 형식은 맞지만 정책/수용량/개수 제한을 충족하지 못한 요청. |
| 429 | AI 일일 요청 한도 초과. 동일 요청 자동 재시도 금지. |
| 503 | 외부 서비스/AI 접수 장애. Error code에 따라 재시도 UI 제공 가능. |

### 1.3 공통 ErrorResponse

```json
{
  "code": "...",
  "message": "...",
  "timestamp": "2026-08-13T...",
  "details": { ... }
}
```

- UI 분기와 로직은 `message` 문자열이 아니라 `code`를 기준으로 구현한다.
- `details`는 오류마다 없을 수 있다. 존재를 가정하지 않는다.
- 하나의 HTTP status에 여러 Error Code가 있어도 실제 응답의 `code`는 항상 하나다. Swagger의 개별 examples를 참고한다.
- AI 서버 내부 오류 Body는 프론트에 직접 노출되지 않는다. 공개 API에서는 Backend 공통 ErrorResponse로 매핑된다.

### 1.4 required / nullable / PATCH

> **required와 nullable은 반대 개념이 아니다**  
> required = 응답 Object에 Key가 반드시 존재한다.  
> nullable = 그 Key의 값으로 null이 허용된다.  
> 예: 로그인 응답의 nickname/birdType, 성향 테스트 동점 응답의 birdType은 required + nullable이다.  
> 배열 응답은 required + non-null이며 데이터가 없으면 `[]`이다.

- PATCH에서 필드 미전달 = 기존 값 유지.
- explicit `null` = 그 필드가 nullable일 때만 값 삭제.
- 배열 `[]` = 배열 비우기. 배열을 비우려고 null을 보내지 않는다.
- non-nullable 필드에 explicit null을 보내면 400 대상이다.
- `version`이 있는 PATCH는 required다. 409 version conflict가 발생하면 최신 데이터를 다시 조회한 뒤 사용자에게 재시도 흐름을 제공한다.

### 1.5 ID, 지역, Enum, 날짜

- `placeId`, `tripId`, `postId`, `previewId`, `jobId`, `fileId` 등 서비스 내부 ID는 int64 의미의 JSON number다. 문자열 ID로 가정하지 않는다.
- `regionCode`는 `SEOUL` 같은 자체 Enum이 아니라 법정동 코드 앞 5자리 시군구 코드 String 한 개다.
- Response의 지역 표준 DTO는 `RegionSummary{sigunguCode, sigunguName}`이며 두 Key 모두 required/non-null이다.
- 여행 생성/AI 생성은 단일 지역만 허용한다. `sigunguCodes[]`는 검색·필터 API에서만 사용한다.
- `sigunguCodes` 미전달/null = 전국, `[]` = 빈 결과, 일부 잘못된 코드가 섞여도 유효 코드만 적용하고 400으로 전체 실패시키지 않는다.
- 날짜 상태 판정과 일일 한도는 Asia/Seoul 기준이다. 서버 date/date-time을 앱 임의 현지 변환해 상태를 다시 계산하지 않는다.

### 1.6 Cursor Pagination

- 네이버 외부 장소 검색을 제외한 내부 목록 API는 cursor 방식이다.
- 첫 요청은 cursor 생략, size 기본 20 / 최대 50, 마지막은 nextCursor=null.
- items는 항상 배열이며 결과가 없으면 `[]`. totalPage/pageNumber 기반 UI를 만들지 않는다.

---

## 2. 서버 상태를 그대로 따라야 하는 UX 공통 규칙

### 2.1 권한/공개 범위

| 상태 | 비소유자/비로그인에게 보이는 내용 | 프론트 처리 |
|---|---|---|
| PUBLIC | 일정/장소/메모/사진 공개 | 일반 공개 UI |
| MEMO_PRIVATE | 일정/장소/사진 공개, 장소 메모만 숨김 | `memo=null`, `memoMasked=true`를 그대로 사용 |
| PRIVATE | 작성자/소유자만 조회 | 비소유자는 404 처리, 존재 여부 노출 금지 |
| BLOCKED Post | 일반 사용자에게 숨김 | 404와 동일하게 처리 |

- 선택적 인증 API는 로그인 상태면 Bearer Token을 함께 보내 사용자별 savedRoute 등 개인화 정보를 받을 수 있다.

### 2.2 Trip 편집 가능 여부는 서버 플래그가 정답

- `routeEditable`, `contentEditable`, `routeLockedReason`을 UI 상태의 기준으로 사용한다.
- 프론트가 날짜나 게시글 visibility를 보고 경로 잠금 여부를 다시 계산하지 않는다.
- 발행된 Post가 연결된 Trip은 Post가 삭제될 때까지 경로(지역/기간/Day/장소/순서)가 잠긴다. Post를 PRIVATE로 바꾸거나 운영상 BLOCKED가 되어도 잠금은 유지된다.
- CANCELLED Trip은 영구 읽기 전용이다. 취소 해제 API는 없다.
- 편집 화면 진입 후 상태가 바뀔 수 있으므로 저장 시 409 `TRIP_ROUTE_LOCKED_BY_PUBLISHED_POST` 또는 `TRIP_CANCELLED_READ_ONLY`를 반드시 처리한다.

### 2.3 같은 장소 중복 배치 금지

> **Trip/AI Preview 전역 placeId 유일성**  
> 하나의 Trip 또는 하나의 AI Preview 전체에서 동일 placeId는 최대 한 번만 존재한다.  
> 같은 장소를 다른 Day에 복제해서 넣는 것도 금지한다.  
> 장소를 다른 Day로 "이동"하거나 동일 Day에서 순서를 변경하는 것은 허용한다.  
> 직접 Trip 추가 중복은 409 `PLACE_ALREADY_ADDED`, Preview 중복은 409 `AI_PREVIEW_PLACE_DUPLICATED`가 될 수 있다.

### 2.4 응답값이 null이어도 Key 자체를 숨기지 않기

- API가 required+nullable로 정의한 Key를 프론트 모델에서 optional key로 바꿔 해석하지 않는다.
- 예: `birdType: null`, `previewRetentionStatus: null`, `weather: null`, `nextCursor: null`은 정상 계약 상태다.

---

## 3. 공통 Enum과 프론트 표시 규칙

Enum code는 API 계약값 그대로 저장/전송한다. 한글 라벨·아이콘은 프론트 로컬 매핑으로 처리할 수 있지만 API code 자체를 번역하거나 다른 문자열로 보내지 않는다.

| 구분 | API 값 | 확정 의미/표시 |
|---|---|---|
| CompanionType | SOLO / COUPLE / FRIENDS / FAMILY / OTHER | 단일 선택, 표시 문구는 UI에서 매핑 |
| Pace | RELAXED / NORMAL / DENSE | 정성적 일정 밀도. 2/3/4곳 고정값이 아님 |
| Visibility | PUBLIC / MEMO_PRIVATE / PRIVATE | 전체 공개 / 메모만 비공개 / 비공개 |
| TravelTheme | ACTIVITY / SNS_HOTPLACE / NATURE / ATTRACTION / SHOPPING / FOOD | 액티비티 / SNS 핫플 / 자연과 함께 / 유명 관광지 / 쇼핑 / 먹방 |
| PersonalityTrait | GOURMET / REST / PHOTO / ACTIVITY / CULTURE | 성향 내부 코드 |
| BirdType | OMOKNUNI / MULCHONGSAE / HOBANSAE / DDAKSAE / DONGBAKSAE | 앱 내부 파트너 새 에셋과 매핑 |

| PersonalityTrait | BirdType | 파트너 새 |
|---|---|---|
| REST | OMOKNUNI | 오목눈이 |
| ACTIVITY | MULCHONGSAE | 물총새 |
| CULTURE | HOBANSAE | 호반새 |
| GOURMET | DDAKSAE | 딱새 |
| PHOTO | DONGBAKSAE | 동박새 |

---

## 4. 인증·온보딩 UX 연동

### 4.1 카카오 로그인

1. React Native 카카오 SDK에서 Kakao Access Token 획득
2. `POST /api/auth/kakao/login`에 `kakaoAccessToken` 전달
3. 서비스 Access/Refresh Token 저장
4. `onboardingCompleted=false`면 성향 테스트로 이동, true면 메인으로 이동

- `isNewUser`는 계정이 이번에 생성됐는지 알려주는 보조 정보일 뿐 화면 이동 기준이 아니다.

### 4.2 성향 테스트

- 질문은 정확히 8개, 각 질문 선택지는 정확히 5개다. 제출 answers도 정확히 8개다.
- 질문 순서는 고정, 각 질문의 options 순서는 호출마다 셔플될 수 있다. 점수 계산을 배열 index/order로 추론하지 않는다.
- `optionId`를 그대로 제출한다.
- 단독 1등: 200 + resultStatus=COMPLETED.
- 동점: 202 + resultStatus=TIE_BREAKER_REQUIRED + `birdType=null` + `tiedTraits`.
- Tie-breaker에서는 tiedTraits 중 하나의 `PersonalityTrait` code를 `selectedTrait`으로 전송한다.
- `trait`, `selectedTrait`, `tiedTraits` 모두 PersonalityTrait Enum code를 사용한다. 별도 label API는 없다.

---

## 5. 이미지 업로드 - 프론트 필수 전처리

> **원본 파일을 그대로 S3에 올리지 않는다**  
> 사용자가 선택할 수 있는 원본: JPG/PNG/WEBP, 원본 장당 최대 5MB.  
> 프론트에서 EXIF(GPS 포함) 제거 → 가로 최대 1080px → WebP 변환 → 최종 500KB 이하로 압축.  
> Presigned URL 요청은 최종 WebP의 contentType=image/webp, sizeBytes, width, height를 기준으로 한다.  
> S3에 올라가는 파일은 최적화된 WebP 한 개뿐이다. JPG/PNG 원본은 서버/S3에 보관하지 않는다.

1. 로컬에서 파일 형식/원본 5MB 이하 확인
2. EXIF 제거, resize, WebP 변환/압축
3. `POST /api/files/presigned-uploads`
4. 응답 uploadUrl로 S3 직접 PUT
5. `POST /api/files/{fileId}/complete`로 완료 검증
6. 이후 Post/TripPlace DTO에 fileId 연결

- 화면 표시용 `ImageSummary`는 `fileId`, `imageUrl`만 사용한다. `objectKey`는 Presigned 업로드 전용 응답에서만 사용한다.

---

## 6. 장소 검색·저장·위시리스트 공통 UX

### 6.1 외부 검색 결과와 placeId resolve

> **검색 결과의 placeId가 null일 수 있음**  
> 네이버 검색 결과가 아직 내부 DB에 없으면 `placeId=null`로 내려올 수 있다.  
> 검색 결과를 보여주는 것만으로 DB 장소를 생성하지 않는다.

저장/위시리스트/Trip Day 추가 등 내부 placeId가 필요한 액션 직전에 `POST /api/places/resolve`를 호출한다.  
resolve 성공 후 받은 placeId로 실제 저장/추가 API를 호출한다.

```text
검색 결과 선택
├─ placeId != null → 바로 다음 액션
└─ placeId == null → POST /api/places/resolve → placeId 확보 → 다음 액션
```

### 6.2 저장 장소와 Trip 위시리스트는 UX 의미가 다름

| 개념 | 사용자 의미 | AI에서의 의미 |
|---|---|---|
| SAVED_PLACES | 평소 저장한 장소: "이런 곳을 좋아해" | 선호 참고 신호. MUST_INCLUDE 아님 |
| TRIP_WISHLIST | 특정 여행에서 가고 싶은 장소: "이번엔 꼭 갈 곳" | 모두 MUST_INCLUDE |

- Trip Day에 장소가 실제 배치되면 같은 장소는 해당 Trip 위시리스트에서 자동 제거된다.
- 하나의 Trip에서 동일 장소가 Day와 위시리스트에 동시에 존재하지 않는다.

---

## 7. 여행 일정 UX 계약

### 7.1 직접 일정 생성

- 입력 흐름: 지역 → 날짜 → 동행 유형 → 여행 테마 → 일정 밀도 → 일정 생성.
- 지역은 5자리 시군구 코드 1개. 테마는 1~3개. Pace는 RELAXED/NORMAL/DENSE.
- 여행 기간 최대 일수 제한은 없다. 프론트에서 7박 8일 제한을 재도입하지 않는다.
- 제목은 시스템이 자동 생성하며 최초 생성 시 Day별 장소는 비어 있다.

### 7.2 Trip 상태

| status | 의미 | UI 주의 |
|---|---|---|
| UPCOMING | 오늘 < startDate | 예정 |
| IN_PROGRESS | startDate ≤ 오늘 ≤ endDate | 진행 |
| COMPLETED | endDate < 오늘 | 완료 |
| CANCELLED | cancelledAt 존재 | 날짜보다 우선, 영구 읽기 전용 |

- 상태 계산은 서버 결과를 신뢰한다. 앱 로컬 시각만으로 재계산해 편집 가능 여부를 판단하지 않는다.
- CANCELLED Trip은 내 여행 목록/캘린더에서 제외되지만 소유자는 직접 상세 조회로 확인할 수 있다.

### 7.3 Day 장소 UX

- Day별 최대 장소 수는 15개.
- 순서 변경은 Day 전체 orderedTripPlaceIds를 전송한다.
- 발행 Post로 경로가 잠기면 장소 추가/삭제/순서 변경/지역·기간 변경 버튼을 비활성화한다. 서버 409도 항상 처리한다.
- 장소 메모/사진은 경로 자체가 아니므로 발행 Post가 있어도 수정 가능하지만 CANCELLED Trip에서는 수정 불가.

---

## 8. 여행 기록(Post)·커뮤니티 UX 계약

### 8.1 작성/임시저장/발행

- `POST /api/posts` 하나만 사용하며 DRAFT/PUBLISHED용 API를 나누지 않는다.
- `publish=false`는 DRAFT: title/content가 null 또는 비어 있어도 저장 가능.
- `publish=true`는 발행: title/content를 서버가 필수 검증한다.
- 제목 최대 50자, 본문 최대 2,000자, 이미지 최대 10장, 해시태그 최대 5개/각 10자.
- DRAFT는 Trip 경로를 잠그지 않는다. 최초 발행 시점부터 연결 Trip 경로가 잠긴다.
- 발행 후 visibility를 PRIVATE로 바꿔도 경로 잠금은 유지된다. Post 삭제 시 경로 잠금이 해제된다.

### 8.2 PostDetailResponse

- top-level에는 region/companionType/themes/category/address/latitude/longitude/publishedAt/createdAt을 별도 반환하지 않는다.
- 장소는 `places[]` 안의 표준 필드(placeId, name, category, address, latitude, longitude, dayNumber, order, memo, memoMasked, images)로 받는다.
- 경로는 `route[] -> {dayNumber, routePoints[{latitude,longitude}]}`. Directions/거리/예상시간 데이터가 아니다.

### 8.3 조회/저장/공유 UX

- 비로그인도 PUBLIC/MEMO_PRIVATE Post 상세 및 ALL/POPULAR 커뮤니티 조회 가능.
- FOLLOWING 탭은 로그인 필수.
- 조회수는 로그인 사용자 기준 24시간에 한 번만 집계하며 작성자/비로그인은 증가하지 않는다.
- 공유수는 공유 버튼 클릭 기준. 실제 공유 완료 여부와 분리된다.

---

## 9. AI 추천 UX - 가장 중요한 연동 규칙

### 9.1 공개 AI 요청 유형

| requestType | 사용 시나리오 | 필수 정책 |
|---|---|---|
| GENERAL | 일반 조건으로 새 AI 일정 추천 | 새 장소 추천이 본체 |
| SAVED_PLACES | 저장 장소 취향을 참고한 새 추천 | savedPlaceIds 최소 1개, 참고 신호 |
| TRIP_WISHLIST | 기존 Trip + 위시리스트를 다시 배치 | 기존 장소 KEEP + wishlist MUST_INCLUDE |

### 9.2 공통 요청 조건

- regionCode: 5자리 시군구 한 개
- startDate/endDate를 사용하며 travelDays를 프론트가 보내지 않는다.
- companionType 단일, themes 1~3개, pace=RELAXED/NORMAL/DENSE.
- pace는 하루 장소 수 2/3/4의 고정 수치가 아니다.

### 9.3 비동기 Job UX

```text
POST 추천 요청
→ Backend 202 + jobId + status=QUEUED
→ GET /api/ai/trip-recommendations/{jobId} polling
→ QUEUED / PROCESSING / SUCCEEDED / FAILED / EXPIRED
→ SUCCEEDED + previewAvailable=true → previewId로 Preview 조회
```

| Backend status | UI 의미 | 핵심 필드 |
|---|---|---|
| QUEUED | 접수 대기 | previewId=null, previewRetentionStatus=null |
| PROCESSING | AI 처리 중 | previewAvailable=false |
| SUCCEEDED | Backend 검증+Preview 저장 완료 | previewId 사용 가능 |
| FAILED | AI/Callback/검증/저장 실패 | status API의 error 표시. HTTP 오류와 별개 |
| EXPIRED | TEMPORARY Preview 만료 | previewAvailable=false, 상세 접근은 410 |

- 정확한 polling interval은 API 계약에서 고정하지 않았다. 과도한 고빈도 호출을 피하고 앱 정책으로 backoff를 적용한다.
- AI `COMPLETED`는 Backend `SUCCEEDED`가 아니다. Backend 검증/Preview 저장까지 끝나야 프론트에 SUCCEEDED로 보인다.

### 9.4 TRIP_WISHLIST 전체 재구성

> **existingSchedule은 프론트 입력이 아니다**  
> 프론트는 tripId와 requestType, allowAdditionalRecommendations만 공개 API 규격대로 전달한다.  
> Spring Boot가 DB의 현재 TripDay/TripPlace로 existingSchedule을 구성해 AI에 전달한다.

기존 장소는 모두 유지하지만 기존 Day/order는 고정이 아니다. AI가 전체 Day/order를 다시 구성할 수 있다.  
AI 결과는 신규 장소만이 아니라 기존 장소+위시리스트+추가 추천을 포함한 전체 일정이다.

- 필수 장소 수가 `Day 수 × 15`를 초과하면 AI 호출 전 `422 AI_ROUTE_REQUIRED_PLACE_LIMIT_EXCEEDED`. 이 경우 사용자가 위시리스트/기존 일정을 줄여야 한다.
- `allowAdditionalRecommendations=false`는 "새 보완 장소를 추가하지 않음"이다. 기존 장소나 MUST_INCLUDE 위시리스트를 제거한다는 의미가 아니다.

### 9.5 Preview 생명주기와 두 개의 버튼

| 사용자 액션 | 무엇이 바뀌나 | 주의 |
|---|---|---|
| 경로 저장 | 같은 previewId를 PERMANENT로 전환 + SavedRoute 생성 | Preview는 계속 편집 가능 |
| 내 여행에 담기/적용 | GENERAL/SAVED_PLACES는 새 Trip 생성, TRIP_WISHLIST는 기존 Trip 갱신 | Preview와 Trip은 이후 서로 자동 동기화하지 않음 |
| 저장 경로 취소 | 마지막 SavedRoute가 사라지면 Preview를 TEMPORARY로 강등 | 취소 시점+24시간 후 만료 가능 |

- 새 Preview는 기본 TEMPORARY이며 24시간 유효. 수정한다고 expiresAt이 연장되지는 않는다.
- PERMANENT Preview는 저장 후에도 제목/설명/해시태그/Day 장소/순서 수정 가능.
- Preview 수정에서 region/date/companion/themes/pace는 바꾸지 않는다. 바꾸려면 새 AI 추천을 요청한다.
- `AI_CALLBACK_TIMEOUT` 후 늦은 COMPLETED가 와도 Backend FAILED는 유지되므로 프론트는 FAILED를 완료 상태로 뒤집어 기대하지 않는다.

---

## 10. 기타 화면별 공통 규칙

### 10.1 지도/경로

- Day route는 order 순 좌표를 반환하고 프론트가 직선/점선으로 연결한다.
- MVP에서 이동수단, 총거리, 예상 이동시간, 네이버 Directions 경로는 제공하지 않는다.

### 10.2 홈/날씨

- 날씨 공급자는 Open-Meteo. 위치 좌표가 없으면 서버가 서울 좌표를 기본값으로 사용한다.
- 날씨 장애 시 Home API 전체 실패가 아니라 `weather=null`, 200을 유지한다. UI는 "날씨 정보를 불러올 수 없습니다."와 같은 실패 상태를 표시한다.
- 섹션별 실패는 `unavailableSections`로 받을 수 있으므로 홈 전체를 에러 화면으로 바꾸지 않는다.
- 낮/밤 표현은 프론트가 06:00~18:00을 낮으로 판단한다.

### 10.3 축제/이벤트

- MVP에서는 UPCOMING/ONGOING만 노출하며 종료된 축제는 목록에서 제외.
- 축제 저장/찜 기능은 MVP 제외.

### 10.4 포토맵

- 발행된 Post의 장소만 방문으로 인정한다. Trip에만 넣은 장소는 방문으로 집계하지 않는다.
- 방문 지역 단위는 5자리 시군구.

### 10.5 차단/팔로우

- 차단 관계가 어느 방향으로든 존재하면 서로의 게시글/프로필/팔로우 목록 노출과 신규 상호작용을 제한한다.
- 차단 시 기존 저장 경로는 sourceAvailable=false로 처리되어 원본 내용이 숨겨질 수 있다.

---

## 11. 프론트 API Client 구현 규칙

- Base client는 Bearer Token 주입, ErrorResponse 파싱, 204 처리, refresh Single-flight를 공통화한다.
- HTTP status만 보고 도메인 분기하지 말고 ErrorResponse.code를 함께 본다.
- 선택적 인증 Endpoint는 로그인 중이면 Token을 보내고, 비로그인이면 인증 없이 호출한다.
- required+nullable Key를 타입 모델에서 optional로 바꾸지 않는다. 예: `birdType?:` 대신 `birdType: BirdType | null` 형태를 우선한다.
- Array Response는 null 방어보다 `[]`를 정상값으로 취급한다.
- PATCH helper가 undefined(미전달)와 null(explicit clear)을 구분하도록 한다.
- 204 Response는 content-length가 없어도 정상 성공으로 처리한다.
- Cursor list는 nextCursor=null이면 종료. total count가 필요하다는 가정으로 무한 스크롤을 설계하지 않는다.
- version 기반 PATCH는 현재 화면 데이터의 version을 보내고 409 시 최신 조회 후 사용자 충돌 안내를 제공한다.
- 멱등 endpoint(저장/취소/차단/팔로우 일부 등)는 반복 탭해도 상태가 깨지지 않지만 UI 자체는 중복 요청을 최소화한다.
- 서버가 소유권/공개범위를 최종 판단하므로 UI에서 숨겼더라도 403/404/409를 항상 처리한다.

### 11.1 프론트에서 임의로 하지 말아야 할 것

- regionCode를 SEOUL/BUSAN 같은 문자열 Enum으로 변환해 전송하지 않기
- placeId/jobId를 String으로 변환해 계약을 바꾸지 않기
- Trip 편집 가능 여부를 날짜/화면 상태로 재계산하지 않기
- AI pace를 2/3/4 장소 수로 고정하지 않기
- SAVED_PLACES를 MUST_INCLUDE로 취급하지 않기
- TRIP_WISHLIST의 필수 장소를 allowAdditionalRecommendations=false 때문에 빼지 않기
- AI 서버/internal API를 앱에서 직접 호출하지 않기
- 이미지 원본을 그대로 Presigned URL에 업로드하지 않기
- MEMO_PRIVATE에서 사진까지 숨기지 않기
- PRIVATE/BLOCKED의 404를 "존재하지만 권한 없음"으로 사용자에게 노출하지 않기

---

## 12. API 계약이 아닌 프론트 구현 선택사항

다음은 현재 API/기능명세에서 숫자나 카피가 고정되지 않은 영역이다. 프론트에서 구현할 수 있지만 API 계약으로 오해하지 않는다.

- AI Job polling의 정확한 초 단위 interval/backoff
- 로딩 스켈레톤/애니메이션/Toast의 구체 표현
- Pace의 화면 한글 라벨/보조 문구(코드는 RELAXED/NORMAL/DENSE로 고정)
- 공통 ErrorResponse.message를 그대로 노출할지 UX 친화 카피로 매핑할지
- 일부 retry 버튼의 시각적 위치/횟수 제한(서버 정책을 변경하지 않는 범위)

---

## 13. 프론트 호출 API 전체 인덱스

최신 OpenAPI의 전체 70개 Operation 중 프론트가 호출하는 공개 Operation 67개를 모두 인덱싱했다. 프론트 호출 금지 Internal API 3개는 별도로 분리했다.

### AI 추천

| Method | Path | 기능 | Auth |
|---|---|---|---|
| GET | `/api/ai/trip-previews/{previewId}` | AI 일정 미리보기 조회 | JWT 필수 |
| PATCH | `/api/ai/trip-previews/{previewId}` | AI 일정 미리보기 수정 | JWT 필수 |
| POST | `/api/ai/trip-previews/{previewId}/apply` | AI 일정 미리보기 확정 적용 | JWT 필수 |
| POST | `/api/ai/trip-recommendations` | AI 일정 추천 요청 | JWT 필수 |
| GET | `/api/ai/trip-recommendations/{jobId}` | AI 작업 상태 조회 | JWT 필수 |
| POST | `/api/trips/{tripId}/ai-route-recommendations` | 일정 위시리스트 기반 AI 경로 배치 요청 | JWT 필수 |

### 게시글/커뮤니티

| Method | Path | 기능 | Auth |
|---|---|---|---|
| POST | `/api/posts` | 여행 기록 작성/임시저장/발행 | JWT 필수 |
| DELETE | `/api/posts/{postId}` | 여행 기록 삭제 | JWT 필수 |
| GET | `/api/posts/{postId}` | 게시글 상세 조회 | 선택 인증 |
| PATCH | `/api/posts/{postId}` | 여행 기록 수정 | JWT 필수 |
| POST | `/api/posts/{postId}/shares` | 공유수 증가 | 선택 인증 |
| POST | `/api/posts/{postId}/views` | 조회수 증가 | 선택 인증 |
| POST | `/api/reports` | 게시글 신고 | JWT 필수 |

### 마이페이지

| Method | Path | 기능 | Auth |
|---|---|---|---|
| GET | `/api/users/me/mypage` | 마이페이지 집계 조회 | JWT 필수 |

### 사용자/프로필

| Method | Path | 기능 | Auth |
|---|---|---|---|
| DELETE | `/api/users/me` | 회원 탈퇴 | JWT 필수 |
| GET | `/api/users/me` | 내 정보 조회 | JWT 필수 |
| GET | `/api/users/me/partner-bird` | 파트너 새 조회 | JWT 필수 |
| GET | `/api/users/me/posts` | 내 게시글 목록 | JWT 필수 |
| PATCH | `/api/users/me/profile` | 프로필 수정 | JWT 필수 |
| GET | `/api/users/me/travel-calendar` | 여행 캘린더 | JWT 필수 |
| GET | `/api/users/me/trips` | 내 여행 목록 | JWT 필수 |

### 소셜/차단

| Method | Path | 기능 | Auth |
|---|---|---|---|
| GET | `/api/users/me/blocked-users` | 차단 사용자 목록 | JWT 필수 |
| DELETE | `/api/users/{targetUserId}/block` | 사용자 차단 해제 | JWT 필수 |
| PUT | `/api/users/{targetUserId}/block` | 사용자 차단 | JWT 필수 |
| DELETE | `/api/users/{targetUserId}/follow` | 언팔로우 | JWT 필수 |
| PUT | `/api/users/{targetUserId}/follow` | 팔로우 | JWT 필수 |
| GET | `/api/users/{userId}/follows` | 팔로워·팔로잉 목록 | JWT 필수 |

### 여행 일정

| Method | Path | 기능 | Auth |
|---|---|---|---|
| POST | `/api/trips` | 직접 일정 생성 | JWT 필수 |
| GET | `/api/trips/{tripId}` | 일정 상세 조회 | 선택 인증 |
| PATCH | `/api/trips/{tripId}` | 일정 기본 정보 수정 | JWT 필수 |
| POST | `/api/trips/{tripId}/cancel` | 여행 취소 | JWT 필수 |
| PUT | `/api/trips/{tripId}/days/{dayNumber}/place-orders` | Day 장소 순서 변경 | JWT 필수 |
| POST | `/api/trips/{tripId}/days/{dayNumber}/places` | Day별 장소 추가 | JWT 필수 |
| DELETE | `/api/trips/{tripId}/days/{dayNumber}/places/{tripPlaceId}` | Day별 장소 삭제 | JWT 필수 |
| GET | `/api/trips/{tripId}/days/{dayNumber}/route` | Day별 이동 경로 조회 | 선택 인증 |
| PATCH | `/api/trips/{tripId}/places/{tripPlaceId}/content` | 장소별 메모·사진·부가정보 수정 | JWT 필수 |

### 온보딩

| Method | Path | 기능 | Auth |
|---|---|---|---|
| GET | `/api/onboarding/personality-test` | 성향 테스트 문항 조회 | JWT 필수 |
| POST | `/api/onboarding/personality-test/submissions` | 성향 테스트 제출 | JWT 필수 |
| POST | `/api/onboarding/personality-test/submissions/tie-breaker` | 성향 테스트 동점 선택 | JWT 필수 |

### 위시리스트

| Method | Path | 기능 | Auth |
|---|---|---|---|
| GET | `/api/trips/{tripId}/wishlist-places` | 일정 위시리스트 조회 | JWT 필수 |
| POST | `/api/trips/{tripId}/wishlist-places` | 일정 위시리스트 장소 추가 | JWT 필수 |
| DELETE | `/api/trips/{tripId}/wishlist-places/{placeId}` | 일정 위시리스트 장소 삭제 | JWT 필수 |

### 이미지

| Method | Path | 기능 | Auth |
|---|---|---|---|
| POST | `/api/files/presigned-uploads` | S3 Presigned 업로드 URL 발급 | JWT 필수 |
| POST | `/api/files/{fileId}/complete` | S3 업로드 완료 확인 | JWT 필수 |

### 인증

| Method | Path | 기능 | Auth |
|---|---|---|---|
| POST | `/api/auth/kakao/login` | 카카오 회원가입 및 로그인 | 불필요 |
| POST | `/api/auth/logout` | 로그아웃 | JWT 필수 |
| POST | `/api/auth/token/refresh` | JWT 토큰 재발급 | 불필요 |

### 장소

| Method | Path | 기능 | Auth |
|---|---|---|---|
| POST | `/api/places/resolve` | 외부 장소 내부 ID 확정 | JWT 필수 |
| GET | `/api/places/search` | 장소 검색 | JWT 필수 |
| GET | `/api/places/{placeId}` | 장소 상세 조회 | JWT 필수 |

### 저장 경로

| Method | Path | 기능 | Auth |
|---|---|---|---|
| GET | `/api/users/me/saved-routes` | 저장한 경로 목록 | JWT 필수 |
| DELETE | `/api/users/me/saved-routes/ai-previews/{previewId}` | AI 미리보기 경로 저장 취소 | JWT 필수 |
| PUT | `/api/users/me/saved-routes/ai-previews/{previewId}` | AI 미리보기 경로 저장 | JWT 필수 |
| DELETE | `/api/users/me/saved-routes/posts/{postId}` | 게시글 경로 저장 취소 | JWT 필수 |
| PUT | `/api/users/me/saved-routes/posts/{postId}` | 게시글 경로 저장 | JWT 필수 |

### 저장 장소

| Method | Path | 기능 | Auth |
|---|---|---|---|
| GET | `/api/users/me/saved-places` | 저장 장소 목록 | JWT 필수 |
| DELETE | `/api/users/me/saved-places/{placeId}` | 장소 저장 취소 | JWT 필수 |
| PUT | `/api/users/me/saved-places/{placeId}` | 장소 저장 | JWT 필수 |
| PATCH | `/api/users/me/saved-places/{placeId}/memo` | 저장 장소 메모 수정 | JWT 필수 |

### 축제/이벤트

| Method | Path | 기능 | Auth |
|---|---|---|---|
| GET | `/api/events` | 진행·예정 축제 목록 | 선택 인증 |
| GET | `/api/events/{eventId}` | 축제 상세 조회 | 선택 인증 |

### 커뮤니티

| Method | Path | 기능 | Auth |
|---|---|---|---|
| GET | `/api/community/posts` | 전체·인기·이웃새 게시글 조회 | 선택 인증 |
| GET | `/api/community/posts/search` | 커뮤니티 검색 | 선택 인증 |

### 통합 검색

| Method | Path | 기능 | Auth |
|---|---|---|---|
| GET | `/api/search` | 장소·기록·지역 통합 검색 | JWT 필수 |

### 포토맵

| Method | Path | 기능 | Auth |
|---|---|---|---|
| GET | `/api/users/me/photomap/regions` | 전국 포토맵과 방문 지역 수 | JWT 필수 |
| GET | `/api/users/me/photomap/regions/{regionCode}/places` | 지역별 방문 장소 | JWT 필수 |

### 홈

| Method | Path | 기능 | Auth |
|---|---|---|---|
| GET | `/api/home` | 홈 데이터 조회 | 선택 인증 |

### 13.1 프론트 호출 금지 Internal API

| Method | Path | 호출 주체 | 인증 |
|---|---|---|---|
| PUT | `/internal/v1/places/sync` | Spring Boot → AI | X-Internal-AI-Key |
| POST | `/internal/v1/recommendations` | Spring Boot → AI | X-Internal-AI-Key |
| POST | `/internal/ai-callbacks/trip-recommendations` | AI → Spring Boot | X-Internal-AI-Key |

---

## 14. 화면/기능 연동 체크리스트

| 영역 | 프론트 완료 기준 |
|---|---|
| 로그인 | onboardingCompleted 기준 분기, nickname/birdType null 처리, refresh single-flight |
| 성향 테스트 | 8문항/5선택지/8답변, optionId 기반, 202 tie-breaker, birdType null |
| 장소 검색 | placeId=null 가능, 액션 직전 resolve, externalPlaceId와 placeId 구분 |
| 저장/위시리스트 | SAVED_PLACES 참고 vs TRIP_WISHLIST 필수 포함 의미 분리 |
| Trip 생성 | 단일 시군구, 테마 1~3, Pace 3종, 기간 상한 없음, 빈 Day 생성 |
| Trip 상세 | routeEditable/contentEditable/routeLockedReason 서버값 사용, CANCELLED read-only |
| 이미지 | EXIF 제거/1080px/WebP/500KB → Presigned → complete |
| Post | publish=false DRAFT, publish=true 검증, 발행 후 Trip route lock |
| 공개 범위 | MEMO_PRIVATE는 메모만 mask, PRIVATE/BLOCKED는 404 |
| AI | 202 jobId → polling → Preview, 5개 Backend status, FAILED는 상태 응답으로도 표현 |
| AI Preview | 24h TEMPORARY, 경로 저장=PERMANENT, apply와 저장은 별도, 적용 후 자동 sync 없음 |
| 지도 | Day 순서 좌표 직선/점선, Directions/거리/시간 없음 |
| 홈 | weather=null/partial failure 처리, unavailableSections |
| 목록 | cursor/size/nextCursor, 빈 items=[] |
| PATCH | undefined vs null vs [], version conflict 409 처리 |

---

## 15. 최종 3단계 검증 결과

### 1단계 - 범위/누락 검증

- OpenAPI의 프론트 호출 Operation 67개를 전부 Appendix 인덱스에 포함했다.
- Internal AI 3개를 별도로 분리해 프론트 호출 금지로 명시했다.
- 인증, required/nullable, PATCH, ErrorResponse, ID/지역/Enum, Cursor, 이미지, Trip, Post, AI, 공개범위 등 횡단 규칙을 모두 포함했다.
- 기능명세서의 프론트 영향 도메인(인증/온보딩/장소/Trip/Post/커뮤니티/저장/포토맵/홈/소셜/검색/이벤트/지도/AI)을 전부 확인했다.

### 2단계 - OpenAPI ↔ 기능명세 ↔ UX 일관성 검증

- 폐기 계약(`travelDays`, `scheduleDensity`, `existingPlaceMode`, 방문 시간/체류시간 필드, 7박 8일 상한)을 활성 프론트 규칙으로 사용하지 않았다.
- SAVED_PLACES와 TRIP_WISHLIST, AI 4 상태와 Backend 5 상태, Preview 저장과 Trip 적용을 서로 다른 개념으로 유지했다.
- `KakaoLoginResponse` required+nullable, 204 no body, `AiJobStatusResponse.previewRetentionStatus` nullable, PostDetail places/route 구체 DTO 등 최신 OpenAPI 수정사항을 반영했다.
- 서버가 결정해야 하는 권한/잠금/상태를 프론트가 재추론하지 않도록 규칙을 일관되게 정리했다.

### 3단계 - Markdown 변환/가독성 검증

Markdown 원문을 기준으로 제목, 본문, 표, 코드/Endpoint, API 인덱스가 누락되지 않았는지 확인한다.  
특히 공개 Operation 67개와 프론트 호출 금지 Internal API 3개가 구분되어 있는지 확인한다.

> **함께 참조하는 기준 파일**
>
> 1. `travelbird-openapi-v10.json`
> 2. `frontend-api-ux-guide-v10.md`
> 3. `backend-functional-spec-v10.md`
> 4. Figma 최신 화면 파일/링크(레이아웃 확인용)
