# 트래블버드 통합 백엔드 기능명세서 결정사항 반영본 v10

> 최종 결정사항 통합본 v10  
> 기준일: 2026-08-13

## 변경 표시 기준

- `[채택·잔여 리스크]`: MVP에서 채택했으나 운영상 잔여 리스크와 주의사항을 함께 기록한 내용
- `[MVP 제외]`: 기존 명세에 있었으나 MVP 범위에서 제거한 기능
- 표시가 없는 내용은 기존 명세의 방향을 유지한 내용이다.
- 기존 문서에서 취소선으로 제거된 내용은 본 통합본에 다시 포함하지 않는다.

---

## 문서 적용 원칙

- API 기본 Prefix는 `/api/`로 한다.
- 서버 내부 시간은 UTC로 저장하고 클라이언트에는 ISO 8601 형식으로 반환한다.
- 날짜 기준 상태 판정과 일일 제한은 대한민국 서비스 기준으로 `Asia/Seoul` 날짜를 사용한다.
- MySQL 문자셋은 `utf8mb4`를 사용한다.
- JPA Entity는 기본적으로 지연 로딩을 사용하며 N 관계는 연결 Entity로 분리한다.
- API 응답에서 JPA Entity를 직접 반환하지 않고 Request·Response DTO를 사용한다.
- 모든 사용자 소유 데이터는 JWT의 사용자 ID를 기준으로 권한을 검증하며 Request Body의 사용자 ID를 신뢰하지 않는다.
- 비로그인 접근이 허용된 API는 선택적 인증을 적용하고, 로그인 필수 API는 인증정보가 없으면 `401 UNAUTHORIZED`를 반환한다.
- API URI, DTO명, Entity명, DB 컬럼명과 타입은 Spring Boot·JPA·MySQL 환경을 기준으로 작성한 구현 초안이다.
- 서비스 내부 지역 식별 기준은 법정동 코드 앞 5자리의 시군구 코드 `String`으로 통일한다. 기존 API의 `regionCode` 필드명은 유지할 수 있으나 값의 의미는 5자리 시군구 코드 하나다.
- 지역 기준정보는 `sigungu_master` 테이블에 관리하고 콘텐츠의 `sigungu_code`와 FK·인덱스로 연결한다. 초기 기준정보는 최신 덤프를 수동 적재하며 MVP에서 코드 변경 자동 수집 시스템은 구현하지 않는다.
- 여행 생성과 AI 일정 추천의 지역은 정확히 1개만 허용한다. 여러 시군구 배열은 검색·필터 API에서만 `sigunguCodes`로 사용한다.
- 검색·필터의 `sigunguCodes`는 미전달 또는 `null`이면 전국 조회, 빈 배열이면 빈 결과, 유효·잘못된 코드가 섞이면 잘못된 코드 때문에 `400`을 반환하지 않고 유효 코드만 적용하며 인근 지역으로 자동 확장하지 않는다.
- 지역을 Response에 포함하는 API는 공통 `RegionSummary{sigunguCode: String, sigunguName: String}` DTO를 사용하며 두 필드는 항상 존재하고 `null`을 허용하지 않는다.
- 화면 표시용 공통 `ImageSummary`는 `fileId: Long`, `imageUrl: String` 두 필드만 사용하며 둘 다 required·non-null이다. S3 내부 `objectKey`, bucket, MIME, 크기·용량 정보는 포함하지 않는다. Presigned Upload 전용 Response의 `objectKey`는 별도 목적이므로 유지한다.
- `CompanionType`은 `SOLO | COUPLE | FRIENDS | FAMILY | OTHER`의 단일 선택 Enum으로 통일한다.
- `BirdType` Enum은 `OMOKNUNI | MULCHONGSAE | HOBANSAE | DDAKSAE | DONGBAKSAE`의 5개 값으로 고정한다.
- `TravelTheme`은 `ACTIVITY | SNS_HOTPLACE | NATURE | ATTRACTION | SHOPPING | FOOD`의 6개 Enum으로 통일하고 1~3개를 중복 없이 선택한다.
- `Pace`는 `RELAXED | NORMAL | DENSE`의 3개 Enum으로 통일한다. `pace`는 하루 장소 수를 2/3/4처럼 고정하는 숫자 제한이 아니라 일정 전체의 밀도와 이동 강도를 표현하는 의미적 조건이며, 최종 장소 수와 Day 배치는 AI가 여행 기간·기존 일정·필수 장소·후보 위치·추천 적합도를 함께 고려하여 판단한다.
- 하나의 `Trip` 또는 하나의 AI `Preview` 안에서 동일한 `placeId`는 최대 한 번만 포함할 수 있다. 같은 장소를 같은 Day뿐 아니라 서로 다른 Day에도 중복 배치할 수 없으며, 기존 장소를 다른 Day로 이동하거나 같은 Day 안에서 방문 순서를 변경하는 것은 허용한다.
- 서비스 내부 `placeId`는 Java `Long`, OpenAPI `integer/int64`, JSON `number`로 유지하며 AI 연동에서도 `Integer`로 강제 Casting하지 않는다.
- 장소 외부 식별자 관리 구조는 `places + place_external_ids` 분리 방식으로 확정한다. `places.placeId`는 서비스 내부의 canonical 장소 ID이며, TourAPI·NAVER 등 외부 Provider 식별자는 `place_external_ids`에서 관리한다.
- `place_external_ids`는 `placeId`, `provider`, `externalPlaceId`를 가지며 `(provider, externalPlaceId)`에 Unique 제약을 적용한다. MVP Provider 값은 `KTO_TOUR_API | NAVER`로 고정한다. 하나의 `placeId`에는 서로 다른 Provider의 외부 ID를 여러 개 연결할 수 있다.
- TourAPI `contentId`는 `provider=KTO_TOUR_API`, `externalPlaceId=contentId`로 저장하고 Backend 내부 `placeId`로 직접 사용하지 않는다. 네이버 검색 결과는 `/api/places/resolve` 내부에서 `provider=NAVER`로 처리하므로 공개 Resolve DTO에 `provider` 필드를 추가하지 않는다.
- 서로 다른 Provider의 장소를 같은 canonical `placeId`로 연결하는 자동 매칭은 보수적으로 처리한다. 정규화된 장소명과 정규화된 주소가 모두 일치하고 좌표 간 거리가 50m 이내이며 고신뢰 후보가 정확히 하나일 때만 기존 `placeId`를 재사용한다. 후보가 없거나 복수이거나 하나라도 조건이 불명확하면 잘못된 병합을 피하기 위해 새 `placeId`를 생성한다.
- AI 추천 작업의 `jobId`는 Spring Boot가 생성하며 Java `Long`, OpenAPI `integer/int64`, JSON `number`로 통일한다. AI 서버는 받은 `jobId`를 그대로 즉시 응답과 Callback에 사용한다.
- Backend→AI 내부 추천 요청은 `POST /internal/v1/recommendations` 비동기 접수 방식으로 사용한다. AI 서버는 신규 정상 요청에 `202 Accepted + PENDING + 동일 jobId + acceptedAt`을 즉시 반환한 뒤 작업 완료 시 Spring Boot Callback API로 `COMPLETED` 또는 `FAILED` 결과를 전달한다.
- `POST /internal/v1/recommendations`의 즉시 응답 Status는 `202 | 400 | 401 | 409 | 422`로 확정한다. `400 INVALID_REQUEST`는 필수 필드 누락·타입·형식·Enum·Schema 제약 위반, `401 INVALID_INTERNAL_AI_KEY`는 인증 Header 누락·불일치, `409 AI_JOB_ID_CONFLICT`는 동일 `jobId`에 기존 요청과 다른 Payload가 전달된 경우, `422`는 `PLACE_NOT_FOUND | PLACE_REGION_MISMATCH | CONFLICTING_PLACE_POLICY`의 도메인·정책 방어 검증 실패에 사용한다.
- 동일 `jobId`와 동일 Payload가 `POST /internal/v1/recommendations`에 재전송되면 AI 서버는 새 작업을 중복 생성하지 않고 기존 접수 결과를 `202 Accepted`로 멱등 반환한다.
- 동일 `jobId`의 동일 Payload 여부는 `jobId`를 제외한 `RecommendationJobRequest`의 정규화된 의미 Payload를 SHA-256 fingerprint로 비교하여 판단한다. JSON 객체 Key 순서, 공백, 개행은 무시한다. 순서가 의미 없는 `themes`, `savedPlaceIds`, `wishlistPlaceIds`는 정렬하여 비교하고, `existingSchedule`은 `dayNumber` 기준으로 정규화하되 각 Day의 `placeIds` 순서는 유지한다. 동일 fingerprint면 기존 접수 결과를 `202 Accepted`로 멱등 반환하고, 다르면 `409 AI_JOB_ID_CONFLICT`를 반환한다.
- `POST /internal/v1/recommendations`의 `400/409/422` 즉시 실패 Response도 프로젝트 공통 `ErrorResponse{code, message, timestamp, details}`를 사용하며 별도 `{error:{...}}` 중첩 ErrorBody를 만들지 않는다.
- AI 내부 API의 `400/422` 검증은 방어적 검증이다. 사용자 요청의 소유권, 저장 장소 여부, 위시리스트, 필수 배치 장소 수 `> dayCount × 15` 등 Backend가 알고 있는 정책은 Spring Boot가 AI 호출 전에 우선 검증한다.
- Spring Boot → AI의 `POST /internal/v1/recommendations`와 AI → Spring Boot의 `POST /internal/ai-callbacks/trip-recommendations`는 동일한 정적 Internal API Key 인증 방식을 사용한다. 두 방향 모두 `X-Internal-AI-Key: {TRAVELBIRD_AI_CALLBACK_KEY}` Header를 필수로 전달한다.
- AI Callback은 `POST /internal/ai-callbacks/trip-recommendations` 하나를 사용한다.
- AI Callback 제한시간은 AI 서버의 `202 Accepted`를 확인하여 Backend Job이 `PROCESSING`으로 전환된 시점부터 **180초**로 한다. 180초 안에 `COMPLETED` 또는 `FAILED` Callback이 도착하지 않으면 Backend Job을 `FAILED`로 변경하고 `AI_CALLBACK_TIMEOUT`을 기록한다.
- AI → Backend Callback 전송이 실패하면 **최대 3회 재시도**한다. 최초 전송 이후 재시도 간격은 `2초 → 5초 → 10초`로 한다. 재시도 대상은 Network error, HTTP `408`, `429`, `5xx`로 한정하며 `400`, `401`, `403`, `404`, `422`를 포함한 그 밖의 HTTP 응답에는 재시도하지 않는다.
- 내부 서버 주소는 코드에 하드코딩하지 않는다. Spring Boot → AI 호출 주소는 `AI_SERVER_BASE_URL`, AI → Backend Callback 주소는 `BACKEND_CALLBACK_BASE_URL` 환경변수로 관리하며 `local/dev/prod` 환경별 값을 분리한다.
- `[채택·잔여 리스크]` `TRAVELBIRD_AI_CALLBACK_KEY` 실제 값은 문서·소스코드·Git·로그에 기록하지 않고 배포 환경에서 32자 이상의 랜덤 영문 대소문자+숫자 Secret으로 생성해 관리한다. HTTPS를 사용하고 `local/dev/prod` 환경별 Key를 분리하며 노출 시 즉시 교체한다.
- 네이버 외부 장소 검색을 제외한 내부 목록 API는 Cursor Pagination을 사용한다. 첫 요청은 `cursor`를 생략하고 `size` 기본값은 20, 최대값은 50이며 마지막 페이지는 `nextCursor=null`을 반환한다. `Page` 객체, `totalPage`, `pageNumber`는 사용하지 않는다.
- Cursor Response의 `items`는 required·non-null이며 결과가 없으면 빈 배열 `[]`을 반환하고, `nextCursor`는 required·nullable이다.
- 모든 PATCH 요청에서 필드 미전달은 기존값 유지, 명시적 `null`은 nullable 필드 값 삭제, 빈 배열 `[]`은 배열 비우기를 의미한다. non-nullable 필드에 명시적 `null`을 전달하면 `400`으로 처리한다. 백엔드는 미전달과 explicit `null`을 구분할 수 있게 구현하며 구체 기술은 구현 선택사항이다.
- OpenAPI에서 PATCH의 일반 수정 필드는 optional로 정의하고, `version`처럼 요청 처리·동시성 제어에 반드시 필요한 제어 필드는 required로 정의한다.
- HTTP 성공 상태는 Response Body가 있는 일반 성공 `200 OK`, 새 리소스 생성과 Body 반환 `201 Created`, 비동기 작업 접수 `202 Accepted`, Body 없는 성공 `204 No Content`를 기준으로 사용한다.
- Response에서 계약상 항상 반환하는 Key는 required로 정의한다. 값이 없을 수 있지만 Key가 항상 있으면 required+nullable로, 배열은 required+non-null로 정의하고 값이 없으면 `[]`을 반환한다.
- 공통 `ErrorResponse`는 `code`, `message`, `timestamp`, `details`를 사용한다. 앞의 세 필드는 required이며 `details`만 optional+nullable로 두고 추가 오류 데이터가 있을 때 사용한다.
- API 오류는 오류 종류별 별도 Response Schema를 만들지 않고 공통 `ErrorResponse`를 사용한다.
- 하나의 HTTP Status에서 여러 Error Code가 발생할 수 있어도 실제 `code` 값은 항상 하나의 Error Code만 반환한다. OpenAPI 예시는 `"A / B / C"`처럼 여러 코드를 하나의 문자열로 합치지 않고 오류별 `examples`로 분리한다.
- 게시글 생성은 `POST /api/posts` 하나를 유지하고 DRAFT/PUBLISHED API를 분리하지 않으며 OpenAPI `oneOf`도 사용하지 않는다. `title`, `content`는 unconditional required에서 제외하고 `publish=false`일 때 미완성을 허용하며 `publish=true`일 때 서버에서 필수 검증한다.

---

# 1. 서비스 개요

## 1.1 한 줄 서비스 정의

- 트래블버드는 대한민국 국내 여행에 특화된 여행 일정 계획·기록·공유 서비스다.
- 사용자는 직접 또는 AI의 도움을 받아 여행 일정을 만들고, 여행 중 사진과 메모를 남기며, 여행 후 기록을 완성하여 다른 사용자와 공유할 수 있다.

## 1.2 서비스 목적

- 여행 전에는 직접 일정 또는 AI 추천 일정으로 여행을 계획한다.
- 여행 중에는 일정에 연결된 장소별로 사진과 한줄 메모를 추가한다.
- 여행 후에는 일정과 장소 정보를 기반으로 여행 기록을 완성한다.
- 여행 준비부터 기록까지 하나의 서비스 흐름으로 연결한다.
- 공개된 기록은 커뮤니티에서 다른 사용자에게 공유된다.
- 다른 사용자는 공개 기록을 통해 새로운 장소와 여행 경로를 발견할 수 있다.
- 사용자가 자신의 여행을 사진과 글로 감성적으로 기록하고 보관할 수 있도록 한다.
- 사용자가 방문한 장소와 지역을 포토맵에 축적하여 여행 성취감을 느낄 수 있고 시각적으로 확인한다.
- 온보딩 성향 테스트를 통해 5종의 파트너 새 중 하나를 배정하여 서비스 정체성을 제공한다.
- `[MVP 제외]` 캐릭터 경험치·레벨·성장 단계와 리워드 포인트 기능은 MVP에서 제공하지 않는다.
- 서비스는 전 세계 여행 편의 기능보다 국내 여행 기록·공유의 감성적 경험에 집중한다.

## 1.3 핵심 서비스 도메인

- 인증·회원 도메인은 카카오 로그인, 로그아웃, 회원 탈퇴와 프로필 관리를 담당한다.
- 온보딩·파트너 새 도메인은 성향 테스트와 파트너 새 종류 배정을 담당한다.
- 장소 도메인은 국내 장소 검색, 상세 조회와 장소 저장을 담당한다.
- 일정 도메인은 직접 일정, Day별 장소, 위시리스트와 여행 캘린더를 담당한다.
- 기록 도메인은 여행 기록 작성, 공개 범위, 이미지와 장소 연결을 담당한다.
- 커뮤니티 도메인은 공개 기록 탐색, 검색, 인기 필터와 이웃새 게시글을 담당한다.
- 저장 도메인은 장소 저장과 다른 사용자의 여행 경로 저장을 담당한다.
- 포토맵 도메인은 게시글과 연결된 방문 장소를 지역별로 집계한다.
- 홈 도메인은 추천 장소, 축제·이벤트와 추천 기록을 제공한다.
- AI 도메인은 여행 조건, 저장 장소 또는 위시리스트를 기반으로 Day별 추천 일정을 생성한다.
- 운영 안전 도메인은 게시글 신고, 게시글 차단 상태와 사용자 차단을 담당한다.
- `[MVP 제외]` 관리자 웹 페이지, 관리자 전용 API, 캐릭터 성장과 리워드 포인트 도메인은 MVP 범위에 포함하지 않는다.

## 1.4 기술 아키텍처 요약

- 모바일 클라이언트는 React Native로 구현한다.
- 백엔드는 Spring Boot와 Spring Security를 사용한다.
- 데이터베이스는 MySQL을 사용한다.
- 인증은 JWT Access Token과 Refresh Token을 사용한다.
- 외부 지도와 장소 검색 기능은 네이버 API를 사용한다.
- 인프라는 AWS EC2, RDS, S3 등을 사용한다.
- 권장 요청 흐름은 `React Native → HTTPS/ALB → Spring Boot → RDS·S3·네이버 API·AI 서버`다.
- 초기 서비스는 모듈형 모놀리스 구조로 한다.
- 프론트엔드가 최적화한 최종 WebP 파일만 S3에 저장하고 DB에는 파일 메타데이터와 S3 Object Key를 저장한다. JPG·PNG 원본 파일은 서버 또는 S3에 보관하지 않는다.
- 운영 DB는 AWS RDS MySQL에 배치한다.
- JWT Secret, 네이버 API Key와 DB 비밀번호는 AWS Secrets Manager 또는 Parameter Store에서 관리한다.
- Backend↔AI 내부 통신 인증용 `TRAVELBIRD_AI_CALLBACK_KEY`도 환경변수 또는 AWS Secrets Manager/Parameter Store에서 관리하며 애플리케이션 소스, 저장소와 요청 로그에 실제 Secret 값을 남기지 않는다. Spring Boot→AI 추천 요청과 AI→Spring Boot Callback에서 동일한 Key를 사용하고 `local/dev/prod` 환경별 Secret을 분리한다.
- Spring Boot가 AI 서버를 호출할 때의 Base URL은 `AI_SERVER_BASE_URL`, AI 서버가 Spring Boot Callback을 호출할 때의 Base URL은 `BACKEND_CALLBACK_BASE_URL`로 관리한다. 두 값은 코드에 고정하지 않고 `local/dev/prod` 환경별 설정으로 주입한다.
- Refresh Token은 Redis를 사용하지 않고 MySQL DB에서만 관리한다.
- 백엔드는 AI 서버에 HTTP REST로 추천 작업을 전달한 뒤 생성 결과를 기다리지 않고 연결을 종료한다. AI 서버는 처리가 끝나면 백엔드 내부 Callback API로 결과를 전달하며, 클라이언트는 `jobId`를 이용한 폴링 방식으로 상태를 확인한다.
- 게시글이 최초 발행된 Trip은 해당 Post가 삭제될 때까지 경로 구조를 잠그며, SavedRoute는 잠긴 원본 경로를 참조한다.
- 이미지 원본은 서버에 보관하지 않고 프론트엔드가 최적화한 WebP 파일만 S3에 업로드한다.
- 카카오 로그인 입력은 React Native 카카오 SDK가 발급한 카카오 Access Token 한 가지 방식으로 통일한다.

---

# 2. 주요 이용자 및 권한

## 2.1 비로그인 사용자

- 비로그인 사용자는 커뮤니티의 `ALL`, `POPULAR` 게시글 목록을 조회할 수 있다.
- 비로그인 사용자는 `PUBLIC`, `MEMO_PRIVATE` 게시글 상세를 조회할 수 있다.
- 비로그인 사용자는 `PUBLIC`, `MEMO_PRIVATE` 일정 상세를 공유 URL로 조회할 수 있다.
- 비로그인 사용자가 장소 검색, 장소 상세 조회, 장소 저장, 팔로우, 사용자 차단, 일정 생성·수정, AI 추천 등 로그인 필수 기능을 요청하면 `401 UNAUTHORIZED`를 반환한다.
- 비로그인 사용자의 게시글 조회는 조회수에 집계하지 않는다.
- 비공개 게시글과 비공개 일정은 비로그인 사용자에게 존재 여부를 노출하지 않고 `404`로 처리한다.

## 2.2 로그인 사용자

- 로그인 사용자는 성향 테스트와 파트너 새 배정을 이용할 수 있다.
- 로그인 사용자는 프로필 조회와 수정을 이용할 수 있다.
- 로그인 사용자는 장소를 검색·조회하고 저장하거나 저장 취소할 수 있다.
- 로그인 사용자는 직접 일정을 생성하고 Day별로 장소를 추가·삭제·정렬할 수 있다.
- 로그인 사용자는 장소 위시리스트를 만들 수 있다.
- 로그인 사용자는 여행 기록을 임시 저장하거나 발행하고 수정·삭제할 수 있다.
- 로그인 사용자는 자신의 일정과 게시글을 `PUBLIC`, `MEMO_PRIVATE`, `PRIVATE`로 설정할 수 있다.
- 로그인 사용자는 다른 사용자의 공개 경로를 저장할 수 있다.
- 로그인 사용자는 다른 사용자를 이웃새로 팔로우하거나 차단할 수 있다.
- 로그인 사용자는 자신의 포토맵, 통계, 일정과 캘린더를 조회할 수 있다.
- 로그인 사용자는 AI 일정 추천을 하루 최대 3회 요청할 수 있다.
- 사용자는 자신의 여행 일정, 게시글, 일정 내 장소와 사진만 수정·삭제할 수 있다.
- 발행된 Post가 연결된 Trip은 경로 구조를 수정할 수 없고, 제목·본문·사진·메모·동행 유형·테마·설명 등 경로와 무관한 정보만 수정할 수 있다.
- Spring Security URL 인가 외에도 Service 계층에서 리소스 소유권을 검증한다.

## 2.3 관리자 등 기타 사용자

- `users.role`은 `ROLE_USER`, `ROLE_ADMIN`으로 구분한다.
- 신규 가입자의 기본 권한은 `ROLE_USER`다.
- 운영 관리자 계정은 DB에서 `ROLE_ADMIN`으로 지정할 수 있다.
- `[MVP 제외]` 관리자 웹 페이지와 `/api/admin/**` 전용 API는 구현하지 않는다.
- 사용자용 게시글 신고 API `POST /api/reports`만 최소 기능으로 제공한다.
- 신고 내역은 `reports` 테이블에 저장한다.
- 신고 접수만으로 게시글을 자동 차단하지 않는다.
- 운영자는 DB에서 대상 게시글의 `status`를 `BLOCKED`로 변경하는 SQL을 직접 실행한다.
- 일반 게시글 목록·상세·검색·추천에서는 `status=BLOCKED`인 게시글을 제외한다.

## 2.4 Spring Security 인증 정책

- JWT 인증은 `OncePerRequestFilter`를 상속한 JWT Filter에서 처리한다.
- Filter는 `Authorization: Bearer {accessToken}` 헤더를 검증한다.
- JWT Payload에는 `userId`, `role`, `tokenType`, `iat`, `exp`를 포함한다.
- Access Token은 Stateless하게 검증한다.
- Access Token 만료기간은 30분이다.
- Refresh Token 만료기간은 30일(`2592000`초)이다.
- Refresh Token은 MySQL의 `refresh_tokens` 테이블에 해시값으로 저장한다.
- 사용자당 활성 Refresh Token은 하나만 허용한다.
- 활성 Refresh Token이 이미 존재하면 새로운 Access Token과 Refresh Token을 발급하지 않고 신규 로그인을 차단한다.
- `deviceId`는 사용하지 않는다.
- 로그아웃 시 Refresh Token만 폐기한다.
- `[MVP 제외]` Access Token 즉시 무효화를 위한 Redis 블랙리스트는 구현하지 않는다.
- 로그아웃 이후 기존 Access Token은 최대 30분 동안 유효할 수 있으며 만료시간이 지나면 자연 만료된다.
- 인증 오류는 `AuthenticationEntryPoint`, 권한 오류는 `AccessDeniedHandler`에서 공통 JSON으로 반환한다.

---

# 3. 백엔드 상세 기능 명세

## 3.1 인증 및 회원

### 3.1.1 기능명: 카카오 회원가입 및 로그인

#### 기능 설명

- 사용자는 카카오 계정을 통해 로그인한다.
- 카카오 최초 로그인 사용자는 신규 회원으로 저장한다.
- 로그인 성공 시 서비스 Access Token과 Refresh Token을 발급한다.
- 카카오 인증 입력 방식은 React Native 카카오 SDK가 획득한 `kakaoAccessToken`을 Spring Boot가 검증하는 방식으로 통일한다.
- 카카오 인가 코드와 `redirectUri`를 백엔드 로그인 요청으로 전달하는 방식은 사용하지 않는다.
- 카카오 이메일은 선택 동의로 수집하며 제공되지 않아도 로그인을 허용한다.
- 카카오 프로필 이미지는 수집·저장하지 않는다.
- 서비스 프로필은 온보딩 테스트 후 배정되는 5종의 파트너 새 아이콘을 사용한다.

#### 세부 로직 및 상태 변화

- 클라이언트는 React Native 카카오 SDK로 로그인한 뒤 발급받은 카카오 Access Token을 백엔드에 전달한다.
- 서버는 전달받은 카카오 Access Token으로 카카오 사용자 정보 API를 호출하여 토큰 유효성과 카카오 사용자 고유 ID를 확인한다.
- 카카오 Access Token 원문은 인증 확인에만 사용하고 서버 DB와 로그에 저장하지 않는다.
- 카카오 사용자 고유 ID로 `user_social_accounts`를 조회한다.
- 계정이 없으면 `users`와 `user_social_accounts`를 하나의 트랜잭션에서 생성한다.
- 신규 사용자는 `status=ACTIVE`, `role=ROLE_USER`, `onboardingCompleted=false`, `birdType=null`로 저장한다.
- 카카오에서 이메일이 제공되면 저장하고, 제공되지 않으면 `users.email=null`로 저장한다.
- 가짜 이메일을 생성하지 않는다.
- 기존 사용자는 사용자 상태가 `ACTIVE`인지 검증한다.
- 해당 사용자에게 활성 Refresh Token이 있으면 신규 로그인을 차단한다.
- 활성 Refresh Token이 없을 때만 서비스 Access Token과 Refresh Token을 발급한다.
- Refresh Token 원문은 반환 후 서버에 보관하지 않고 해시값만 저장한다.
- 온보딩 완료 여부를 응답에 포함한다.
- 로그인 후 화면 이동은 `isNewUser`가 아니라 `onboardingCompleted`를 기준으로 한다. `false`면 온보딩으로, `true`면 메인으로 이동하며 `isNewUser`는 이번 로그인에서 계정이 생성되었는지 나타내는 보조 정보로만 사용한다.

#### 입력 / Request DTO

- DTO명은 `KakaoLoginRequest`다.
- `kakaoAccessToken: String`
- `authorizationCode`, `redirectUri`, `deviceId`는 Request DTO에 포함하지 않는다.

#### 출력 / Response DTO

- `accessToken: String`
- `refreshToken: String`
- `accessTokenExpiresIn: Long`
  - Access Token 만료까지 남은 시간(초)
  - 기본값 `1800`초
- `refreshTokenExpiresIn: Long`
  - Refresh Token 만료까지 남은 시간(초)
  - 기본값 `2592000`초
- `isNewUser: Boolean`
- `onboardingCompleted: Boolean`
- `userId: Long`
- `nickname: String?` — Response Key는 required이며 값은 nullable
- `birdType: BirdType?` — Response Key는 required이며 값은 nullable

#### 예외사항 및 검증 로직

- `kakaoAccessToken`이 누락되면 `400 INVALID_AUTH_REQUEST`를 반환한다.
- 카카오 토큰 검증에 실패하면 `401 KAKAO_AUTHENTICATION_FAILED`를 반환한다.
- 정지 계정이면 `403 USER_NOT_ACTIVE`를 반환한다.
- 활성 Refresh Token이 존재하면 `409 ACTIVE_SESSION_ALREADY_EXISTS`를 반환한다.
- 카카오 이메일 미제공은 오류로 처리하지 않는다.
- `provider + providerUserId` 유니크 제약으로 중복 계정 생성을 방지한다.
- 카카오 외부 API 장애 시 `503 KAKAO_SERVICE_UNAVAILABLE`을 반환한다.

#### 연동 API 엔드포인트

- `POST /api/auth/kakao/login`

---

### 3.1.2 기능명: JWT 토큰 재발급

#### 기능 설명

- 만료된 Access Token을 유효한 Refresh Token으로 재발급한다.
- 사용자당 하나의 활성 Refresh Token만 유지한다.

#### 세부 로직 및 상태 변화

- Refresh Token의 서명과 만료시간을 검증한다.
- Refresh Token에서 사용자 ID를 확인한다.
- MySQL DB에 저장된 해당 사용자의 Refresh Token 해시와 비교한다.
- 사용자 상태가 `ACTIVE`인지 확인한다.
- 검증에 성공하면 기존 Refresh Token을 폐기하고 새로운 Access Token과 Refresh Token으로 교체한다.
- 재사용되거나 DB 값과 불일치하는 토큰은 거부한다.
- Redis와 `deviceId`는 사용하지 않는다.
- Refresh Token Rotation 시 프론트엔드는 재발급 요청을 Single-flight로 처리하여 동시에 발생한 401 요청을 대기열에 두고, 새 Access/Refresh Token 저장 후 재시도한다. 이를 위한 별도 백엔드 세션 동기화 서버는 구현하지 않는다.

#### 입력 / Request DTO

- `refreshToken: String`

#### 출력 / Response DTO

- `accessToken: String`
- `refreshToken: String`
- `accessTokenExpiresIn: Long`
  - Access Token 만료까지 남은 시간(초)
- `refreshTokenExpiresIn: Long`
  - Refresh Token 만료까지 남은 시간(초)

#### 예외사항 및 검증 로직

- 만료된 Refresh Token은 `401 REFRESH_TOKEN_EXPIRED`다.
- 폐기된 토큰은 `401 REFRESH_TOKEN_REVOKED`다.
- DB 저장값과 일치하지 않으면 `401 INVALID_REFRESH_TOKEN`이다.
- 정지·삭제 사용자 토큰이면 `403 USER_NOT_ACTIVE`다.

#### 연동 API 엔드포인트

- `POST /api/auth/token/refresh`

---

### 3.1.3 기능명: 로그아웃

#### 기능 설명

- 사용자는 현재 활성 로그인 세션에서 로그아웃한다.

#### 세부 로직 및 상태 변화

- 전달받은 Refresh Token의 소유자와 JWT 사용자 ID가 일치하는지 검증한다.
- DB에서 Refresh Token 레코드를 삭제하거나 `revokedAt`을 기록한다.
- 클라이언트는 기기에 저장된 Access Token과 Refresh Token을 삭제한다.
- Access Token 블랙리스트는 사용하지 않는다.
- Access Token은 발급 후 최대 30분 내 자연 만료된다.

#### 입력 / Request DTO

- `refreshToken: String`

#### 출력 / Response DTO

- 성공 시 `204 No Content`

#### 예외사항 및 검증 로직

- 이미 폐기된 토큰으로 요청해도 멱등하게 `204 No Content`를 반환한다.
- 타인의 Refresh Token이면 `403 TOKEN_ACCESS_DENIED`를 반환한다.

#### 연동 API 엔드포인트

- `POST /api/auth/logout`

---

### 3.1.4 기능명: 회원 탈퇴

#### 기능 설명

- 사용자는 계정을 즉시 탈퇴할 수 있다.
- 탈퇴 후 동일 카카오 계정으로 재가입할 수 있다.

#### 세부 로직 및 상태 변화

- 모든 Refresh Token을 폐기한다.
- 탈퇴 사용자의 공개·비공개 게시글을 삭제 처리한다.
- 게시글과 연결된 이미지 파일을 S3와 DB에서 삭제한다.
- 일정, Day, 일정 장소, 일정 장소 메모·사진, 위시리스트, 저장 장소, 포토맵 대상 데이터를 요청 시점에 삭제한다.
- 팔로우·차단 관계와 사용자 소유 신고 부가정보를 정리한다.
- `user_social_accounts`의 카카오 연결정보를 제거하여 동일 카카오 계정 재가입을 허용한다.
- 게시글 원본을 참조한 타 사용자의 저장 관계는 삭제하지 않고 `sourceAvailable=false`로 변경하여 원본 내용을 노출하지 않는다.
- 대량 S3 삭제는 탈퇴 API 트랜잭션 완료 후 실패 재처리가 가능한 삭제 작업 테이블로 처리할 수 있으나, 사용자 화면에서는 즉시 삭제된 것으로 간주한다.

#### 입력 / Request DTO

- 별도 Request Body 없음

#### 출력 / Response DTO

- 성공 시 `204 No Content`

#### 예외사항 및 검증 로직

- 이미 탈퇴한 요청은 멱등하게 처리한다.
- Access Token 사용자와 탈퇴 대상은 일치해야 한다.

#### 연동 API 엔드포인트

- `DELETE /api/users/me`

---

## 3.2 프로필

### 3.2.1 기능명: 내 정보 조회

#### 기능 설명

- 로그인 사용자의 닉네임, 자기소개와 파트너 새 종류를 조회한다.

#### 세부 로직 및 상태 변화

- JWT 사용자 ID로 `users`를 조회한다.
- 프론트엔드는 `birdType`과 앱 내부 5종 이미지 에셋을 매핑한다.
- 카카오 프로필 이미지 또는 사용자 업로드 프로필 이미지를 사용하지 않는다.

#### 출력 / Response DTO

- `userId: Long`
- `nickname: String?`
- `introduction: String?`
- `onboardingCompleted: Boolean`
- `birdType: BirdType?`
- `role: UserRole`

#### 예외사항 및 검증 로직

- 존재하지 않는 사용자는 `404 USER_NOT_FOUND`다.
- 온보딩 전이면 `birdType=null`을 반환한다.

#### 연동 API 엔드포인트

- `GET /api/users/me`

---

### 3.2.2 기능명: 프로필 수정

#### 기능 설명

- 사용자는 닉네임과 자기소개를 수정할 수 있다.

#### 세부 로직 및 상태 변화

- 변경된 필드만 수정하는 PATCH 방식을 사용한다.
- 닉네임 중복은 허용한다.
- 닉네임은 최소 2자, 최대 10자다.
- 자기소개는 공백 포함 최대 100자다.
- 프로필 이미지 수정 기능은 제공하지 않는다.

#### 입력 / Request DTO

- `nickname: String?`
- `introduction: String?`

#### 출력 / Response DTO

- 수정된 프로필 전체를 반환한다.

#### 예외사항 및 검증 로직

- 닉네임 또는 자기소개 길이가 기준을 벗어나면 `400 INVALID_PROFILE_VALUE`를 반환한다.

#### 연동 API 엔드포인트

- `PATCH /api/users/me/profile`

---

## 3.3 온보딩 및 파트너 새

### 3.3.1 기능명: 성향 테스트 문항 조회

#### 기능 설명

- 성향 테스트는 총 8개 질문과 문항별 5개 선택지로 구성한다.
- 각 선택지는 `미식`, `휴식`, `사진`, `액티비티`, `문화` 중 하나의 성향 점수를 가진다.
- 테스트를 건너뛸 수 없으며 온보딩 완료 전 필수로 수행한다.
- 사용자가 선택지 위치 패턴을 추측하지 못하도록 API 호출마다 각 문항의 선택지 배열 순서를 무작위로 섞어 반환한다.

#### 세부 로직 및 상태 변화

- 활성화된 최신 테스트 버전의 질문을 Q1부터 Q8까지 고정 순서로 반환한다.
- 질문 순서는 유지하고, 각 질문에 속한 5개 선택지만 독립적으로 Random Shuffle한다.
- 선택지의 점수 성향은 `optionId`와 서버 내부 매핑값으로 판정하며, 응답 배열 위치나 표시 순서로 점수를 계산하지 않는다.
- 셔플 이후 클라이언트 표시용 `order`를 1부터 다시 부여한다.
- 결과 점수, 내부 성향 코드와 파트너 새 매핑 기준은 클라이언트에 노출하지 않는다.
- 테스트 문항 변경에 대비해 질문 세트의 `testVersion`을 관리한다.

#### 캐릭터 성향 질문 리스트

1. **Q1. 여행지에서 정체불명의 초대장을 받았다. 초대장에 적혀있으면 하는 문구는?**
   - `[미식]` 아무나 맛볼 수 없는, 오늘 단 하루만 열리는 미식의 정점에 당신을 초대합니다.
   - `[휴식]` 오늘만큼은 서두르지 마세요. 당신만의 완벽한 쉼터로 초대합니다.
   - `[사진]` 셔터를 누르는 순간 영화가 되는 곳, 당신만의 인생 한 장면을 선물해 드립니다.
   - `[액티비티]` 심장이 터질 듯한 새로운 도파민의 세계가 곧 열립니다.
   - `[문화]` 당신의 눈과 귀를 사로잡고 마음에 깊은 영감을 새겨줄 현장으로 초대합니다.

2. **Q2. 여행지에 도착한 직후, 내 눈앞에 펼쳐졌으면 하는 장면은?**
   - `[액티비티]` 지루한 일상을 단번에 날려버릴 만큼 활기차고, 온몸의 세포를 깨워줄 짜릿한 모험과 즐길 거리가 가득한 핫플레이스
   - `[사진]` 창밖으로 수채화처럼 붉게 물들어가는 노을과, 그 실루엣이 액자처럼 담기는 빈티지 카페의 창가
   - `[문화]` 수십 년의 세월을 간직한 골목과 은은한 아날로그 감성이 묻어나는 오래된 문화 거리
   - `[휴식]` 잔잔하게 들리는 파도 소리와 은은한 나무 향이 가득한 프라이빗하고 아늑한 숙소
   - `[미식]` 침샘을 자극하는 맛있는 음식 냄새와, 달콤한 디저트 냄새가 뒤섞인 활기찬 거리

3. **Q3. 숙소 근처를 걷다가 다섯 갈래 길을 발견했다. 어느 길로 가볼까?**
   - `[사진]` 스냅사진 속 한 장면처럼 초록 식물들이 하얀 벽을 감싸고 있는, 카메라를 켤 수밖에 없는 예쁜 주택가 길
   - `[문화]` 수십 년의 세월을 간직한 오래된 건축물과 고즈넉한 돌담길을 따라 느긋하게 걷는 역사적인 길
   - `[휴식]` 따뜻한 온기와 은은한 음악이 기다리는 숙소로 향하는 잔잔하고 편안한 길
   - `[미식]` 코끝을 강렬하게 자극하는 맛있는 음식 냄새를 따라, 현지인들이 옹기종기 줄 서 있는 유명 먹거리 길
   - `[액티비티]` 멀리서 웃음소리와 활기찬 에너지가 뿜어져 나오는, 재미있는 즐길 거리가 가득해 보이는 북적이는 길

4. **Q4. 이번 여행에서 하루 동안 특별한 능력을 하나 얻는다면?**
   - `[휴식]` 시끄러운 인파 속에서도 머릿속이 비워지며 완벽한 이너피스를 찾는 능력
   - `[액티비티]` 아침부터 새벽까지 쉬지 않고 돌아다녀도 방전되지 않는 무한 체력
   - `[문화]` 오래된 돌담길이나 건물만 봐도 그곳에 얽힌 흥미진진한 이야기를 알아채는 능력
   - `[미식]` 간판만 슥 봐도 실패 확률 0%의 로컬 찐맛집을 단번에 감별하는 능력
   - `[사진]` 평범한 골목길에서도 필터 낀 듯 감성적인 구도를 포착해 내는 스냅 작가의 눈

5. **Q5. 이제 이 여행지에서 내게 주어진 시간은 단 3시간. 이 중 제일 포기 못하는 것은?**
   - `[사진]` 3시간이면 인생샷 300장은 건진다! 무조건 뷰가 예쁜 핫플레이스로 직진.
   - `[미식]` 웨이팅이 길어도 상관없다. 이 지역에서 가장 유명한 로컬 맛집으로 달려간다.
   - `[휴식]` 3시간뿐인데 쫓기듯 다니기 싫다… 여기저기 돌아다니지 않고, 편안한 공간에서 좋아하는 음악 들으며 쉬기.
   - `[액티비티]` 지루하게 보낼 틈이 없어! 남은 시간을 꽉 채워줄 활기차고 역동적인 놀거리를 찾아 발 빠르게 움직인다.
   - `[문화]` 이 지역의 진짜 정취를 채우고 싶어, 로컬 감성이 가득한 문화 공간이나 소품샵 투어하기.

6. **Q6. 내가 꿈꾸는 ‘완벽한 여행의 마무리’는?**
   - `[휴식]` 따뜻한 물로 샤워를 마치고, 폭신한 침대 속으로 쏙 들어가, 밀린 피로를 풀며 꿀잠 자기
   - `[액티비티]` 힙한 펍에서 밤새 다트 게임을 즐기거나, 야간 야외 수영장에서 수영하며 보내기
   - `[문화]` 은은한 달빛이 내리는 골목과 문화재 야경을 느긋하게 산책하며, 이 도시의 밤을 온전히 느끼기
   - `[미식]` 숙소 테이블에 지역 유명 야식과 시원한 맥주를 잔뜩 펼쳐놓고 맛있게 먹으며 수다 떨기
   - `[사진]` 스탠드 불빛 아래에서 오늘 찍은 사진들을 쭉 훑어보고, 마음에 드는 인생샷을 골라 SNS나 일기장에 기록하기

7. **Q7. 여행을 마무리하고 탄 기차 안에서 하는 말은?**
   - `[휴식]` “몸도 마음도 제대로 비우고 가네. 다시 시작할 힘이 좀 나는 것 같아.”
   - `[액티비티]` “진짜 알차게 놀았다! 다음엔 더 다이나믹하고 재미 넘치는 곳으로 가야지.”
   - `[문화]` “그때 봤던 작품과 도시의 풍경이 아직도 잊히지 않네.”
   - `[미식]` “그 맛을 잊을 수가 없다... 또 먹으러 가고 싶어!”
   - `[사진]` “사진 보정하고 있는데 버릴 사진이 하나도 없네. 역시 남는 건 사진뿐이야.”

8. **Q8. 여행을 마치고 열어본 내 갤러리는?**
   - `[휴식]` 아늑한 숙소와 조용한 카페에서 온전히 쉬어갔던 편안한 순간들의 흔적
   - `[액티비티]` 온몸을 던져 신나게 뛰어놀며 찍힌, 흔들렸지만 생동감 넘치는 역동적인 순간들
   - `[문화]` 이 지역 고유의 매력이 묻어나는 웅장한 건축물과 독특한 예술 작품이 담긴 사진들
   - `[미식]` 비주얼만 봐도 군침이 도는 푸짐한 로컬 음식과 디저트 사진들로 가득한 화면
   - `[사진]` 카메라 셔터만 눌러도 그림이 되는 포토존에서 남긴 나만의 감성 가득한 인생샷들

> 문서의 `[미식]`, `[휴식]`, `[사진]`, `[액티비티]`, `[문화]` 표시는 백엔드 점수 매핑을 설명하기 위한 내부 표기이며 실제 사용자 응답 DTO의 선택지 텍스트에는 노출하지 않는다.

#### 출력 / Response DTO

- `testVersion: String`
- `questions`
  - `questionId: Long`
  - `text: String`
  - `order: Int`
  - `options`
    - `optionId: Long`
    - `text: String`
    - `order: Int` — 호출 시 셔플된 표시 순서

#### 예외사항 및 검증 로직

- 활성 테스트가 없으면 `503 PERSONALITY_TEST_UNAVAILABLE`을 반환한다.
- 이미 성향 테스트를 완료한 사용자도 문항 조회는 허용한다.
- 단, 완료된 사용자의 테스트 재제출은 허용하지 않는다.

#### 연동 API 엔드포인트

- `GET /api/onboarding/personality-test`

---

### 3.3.2 기능명: 성향 테스트 제출 및 파트너 새 배정

#### 기능 설명

- 사용자가 8개 문항의 답변을 제출하면 선택지별 성향 점수를 합산한다.
- 최고 점수가 하나이면 해당 성향에 연결된 파트너 새를 즉시 배정한다.
- 최고 점수 성향이 둘 이상이면 서버가 임의 우선순위를 적용하지 않고 사용자가 동점 성향 중 하나를 직접 선택하도록 2단계 Tie-breaker 흐름을 사용한다.
- 테스트 재응시와 건너뛰기를 허용하지 않는다.

#### 세부 로직 및 상태 변화

- 테스트 버전과 질문·선택지 유효성을 검증한다.
- 8개 필수 질문에 정확히 한 번씩 답했는지 검증한다.
- 선택지 표시 순서가 아니라 `optionId`에 연결된 내부 성향 코드로 점수를 합산한다.
- 내부 성향 코드는 `GOURMET`, `REST`, `PHOTO`, `ACTIVITY`, `CULTURE`다.
- `PersonalityTrait` Enum은 `GOURMET`, `REST`, `PHOTO`, `ACTIVITY`, `CULTURE`의 5개 값으로 고정한다.
- 성향 테스트 제출 시 `testVersion`은 제출 시점의 최신 활성 테스트 버전과 일치해야 한다.
- 사용자가 과거 버전의 문항을 조회한 뒤 서버의 활성 테스트 버전이 변경된 경우 구버전 `testVersion`을 사용한 제출은 허용하지 않는다.
- 클라이언트는 최신 성향 테스트 문항을 다시 조회한 뒤 새 `testVersion`으로 다시 제출해야 한다.
- 성향 코드와 파트너 새의 1:1 매핑은 다음과 같이 확정한다.

| PersonalityTrait | BirdType | 파트너 새 | 서비스 성향 |
| --- | --- | --- | --- |
| `REST` | `OMOKNUNI` | 오목눈이 | 힐링/휴양 |
| `ACTIVITY` | `MULCHONGSAE` | 물총새 | 액티비티/모험 |
| `CULTURE` | `HOBANSAE` | 호반새 | 문화/예술 |
| `GOURMET` | `DDAKSAE` | 딱새 | 미식/맛집 |
| `PHOTO` | `DONGBAKSAE` | 동박새 | 감성/기록 |

- 단독 최고 점수인 경우 다음을 하나의 트랜잭션으로 처리한다.
  1. 답변과 점수 결과를 저장한다.
  2. 성향에 연결된 `birdType`을 `users.bird_type`에 저장한다.
  3. 제출 상태를 `COMPLETED`로 변경한다.
  4. `users.onboardingCompleted=true`로 변경한다.
- 동점인 경우 다음을 처리한다.
  1. 답변과 성향별 점수를 저장한다.
  2. 제출 상태를 `PENDING_TIE_BREAKER`로 저장한다.
  3. 동점 성향 목록을 제출 레코드에 저장한다.
  4. `users.bird_type`을 배정하지 않는다.
  5. `users.onboardingCompleted=false`를 유지한다.
  6. `202 Accepted`와 `submissionId`, `tiedTraits`를 반환한다.
- Tie-breaker 요청에서는 사용자가 선택한 성향이 해당 제출의 `tiedTraits`에 포함되는지 검증한다.
- Tie-breaker 완료 시 파트너 새 배정과 `onboardingCompleted=true` 변경을 하나의 트랜잭션으로 처리한다.
- 한 번 완료된 제출은 다시 선택하거나 재응시할 수 없다.
- 새 이미지 파일은 백엔드에서 관리하지 않고 프론트 앱 내부 에셋을 사용한다.
- Tie-breaker의 `tiedTraits`와 `selectedTrait`은 내부 `PersonalityTrait` 코드만 사용한다. 별도 `{code,label}` API나 DB는 만들지 않고 화면의 한글 라벨은 프론트엔드가 코드에 맞게 매핑한다.

#### resultStatus 응답 규칙

- 단독 최고점 또는 Tie-breaker 완료 응답의 `resultStatus`는 반드시 `COMPLETED`다.
- 동점 발생 응답의 `resultStatus`는 반드시 `TIE_BREAKER_REQUIRED`다.
- 따라서 `200 OK` 완료 응답에서는 `resultStatus=COMPLETED`, `202 Accepted` 동점 응답에서는 `resultStatus=TIE_BREAKER_REQUIRED`로 고정한다.
- 공통 Enum이 두 값을 모두 정의하더라도 각 응답 상황에서는 해당 상태에 맞는 값 하나만 허용한다.

#### Step 1 입력 / Request DTO

- `testVersion: String`
- `answers: List<PersonalityAnswerRequest>`
  - `questionId: Long`
  - `optionId: Long`

#### Step 1 출력 / Response DTO

- **단독 1등: `200 OK`**
  - `submissionId: Long`
  - `resultStatus: COMPLETED`
  - `onboardingCompleted: true`
  - `birdType: BirdType`
  - `birdName: String`
  - `trait: PersonalityTrait`
  - `description: String`
- **동점: `202 Accepted`**
  - `submissionId: Long`
  - `resultStatus: TIE_BREAKER_REQUIRED`
  - `onboardingCompleted: false`
  - `birdType: null`
  - `tiedTraits: List<PersonalityTrait>`

#### Step 2 입력 / Request DTO

- DTO명은 `PersonalityTieBreakerRequest`다.
- `submissionId: Long`
- `selectedTrait: PersonalityTrait` (`GOURMET | REST | PHOTO | ACTIVITY | CULTURE`)

#### Step 2 출력 / Response DTO

- `200 OK`
- `submissionId: Long`
- `resultStatus: COMPLETED`
- `onboardingCompleted: true`
- `birdType: BirdType`
- `birdName: String`
- `trait: PersonalityTrait`
- `description: String`

#### 예외사항 및 검증 로직

- 답변 수가 8개와 다르면 `400 INCOMPLETE_PERSONALITY_TEST`를 반환한다.
- 동일 질문에 중복 답변하면 `400 DUPLICATED_PERSONALITY_ANSWER`를 반환한다.
- 유효하지 않은 질문·선택지는 `400 INVALID_PERSONALITY_OPTION`이다.
- 제출된 `testVersion`이 현재 최신 활성 테스트 버전과 일치하지 않으면 `409 PERSONALITY_TEST_VERSION_MISMATCH`를 반환한다.
  - 사용자 메시지: `최신 성향 테스트 버전이 아닙니다. 문항을 다시 조회해 주세요.`
- 동점이 아닌 제출에 Tie-breaker를 요청하면 `409 TIE_BREAKER_NOT_REQUIRED`다.
- `selectedTrait`이 저장된 동점 후보 목록에 없으면 `400 INVALID_TIE_BREAKER_SELECTION`이다.
- 이미 Tie-breaker가 완료되었거나 테스트를 완료한 사용자의 재제출은 `409 PERSONALITY_TEST_ALREADY_COMPLETED`다.
- 다른 사용자의 `submissionId`에 접근하면 `403 PERSONALITY_SUBMISSION_ACCESS_DENIED`다.

#### 연동 API 엔드포인트

- Step 1: `POST /api/onboarding/personality-test/submissions`
- Step 2: `POST /api/onboarding/personality-test/submissions/tie-breaker`

#### 성향과 파트너 새 최종 매핑

- `REST` → `OMOKNUNI` → 오목눈이 → 힐링/휴양
- `ACTIVITY` → `MULCHONGSAE` → 물총새 → 액티비티/모험
- `CULTURE` → `HOBANSAE` → 호반새 → 문화/예술
- `GOURMET` → `DDAKSAE` → 딱새 → 미식/맛집
- `PHOTO` → `DONGBAKSAE` → 동박새 → 감성/기록
- 단독 최고점 또는 Tie-breaker 최종 선택으로 확정된 `PersonalityTrait`을 위 매핑에 따라 실제 API `BirdType` 코드로 변환하여 `users.bird_type`에 저장한다.

---

## 3.4 파트너 새

### 3.4.1 기능명: 파트너 새 조회

#### 기능 설명

- 로그인한 사용자의 파트너 새 종류, 이름과 성향 설명을 조회한다.
- 온보딩 전에도 `204 No Content`를 사용하지 않고 `200 OK`와 `birdType=null` 응답을 반환한다.

#### 세부 로직 및 상태 변화

- JWT 사용자 ID로 `users.bird_type`을 조회한다.
- 프론트엔드는 `birdType`에 맞는 앱 내부 이미지 에셋을 표시한다.
- 레벨, 경험치와 성장 단계는 관리하지 않는다.
- 온보딩이 완료되지 않았거나 파트너 새가 아직 배정되지 않았으면 정상 응답으로 모든 파트너 새 상세 필드를 `null`로 반환한다.

#### 출력 / Response DTO

- `onboardingCompleted: Boolean`
- `birdType: BirdType?`
- `birdName: String?`
- `trait: PersonalityTrait?`
- `description: String?`

온보딩 전 응답 예시는 다음과 같다.

```json
{
  "onboardingCompleted": false,
  "birdType": null,
  "birdName": null,
  "trait": null,
  "description": null
}
```

#### 예외사항 및 검증 로직

- 온보딩 전 또는 Tie-breaker 대기 상태는 오류가 아니며 `200 OK`를 반환한다.
- 사용자가 존재하지 않거나 삭제된 경우 `404 USER_NOT_FOUND`를 반환한다.

#### 연동 API 엔드포인트

- `GET /api/users/me/partner-bird`

---

### 3.4.2 기능명: 캐릭터 경험치 및 성장 처리

- `[MVP 제외]` 캐릭터 경험치, 레벨업, 성장 단계와 외형 진화는 MVP에서 완전히 제외한다.
- 별도의 경험치 적립 API, 내부 이벤트 핸들러와 성장 관련 테이블을 구현하지 않는다.

---

## 3.5 이미지 업로드

### 3.5.1 기능명: 이미지 업로드 URL 발급

#### 기능 설명

- 일정 장소와 여행 게시글 이미지를 백엔드를 거치지 않고 S3로 직접 업로드하기 위한 Presigned URL을 발급한다.
- 프로필 이미지는 업로드 대상에 포함하지 않는다.
- 사용자가 선택할 수 있는 원본 파일 형식은 JPG, PNG, WEBP이며 원본 선택 파일은 장당 최대 5MB다.
- 원본 파일 자체는 서버 또는 S3에 업로드하지 않고, React Native 앱이 최적화한 최종 WebP 파일만 S3에 업로드한다.

#### 세부 로직 및 상태 변화

- 프론트엔드는 업로드 전에 다음 순서로 이미지를 처리한다.
  1. JPG, PNG, WEBP 여부와 원본 5MB 이하 여부를 확인한다.
  2. GPS 좌표를 포함한 EXIF 메타데이터를 제거한다.
  3. 가로 크기를 최대 1080px로 축소하고 원본 비율을 유지한다.
  4. WebP로 변환하고 최종 파일을 장당 500KB 이하로 압축한다.
  5. 최적화가 완료된 WebP 파일의 메타정보로 Presigned URL을 요청한다.
- 백엔드가 발급하는 Presigned URL은 `image/webp`, 최대 500KB, 가로 1080px 이하의 최종 파일 업로드에만 사용한다.
- S3에는 최적화된 WebP 한 개만 저장하며 JPG·PNG 원본과 별도 파생 이미지는 저장하지 않는다.
- 장소별 이미지는 최대 3장, 게시글 전체 이미지는 최대 10장이다.
- 발급 시 파일을 `PENDING`으로 저장하고 Presigned URL을 반환한다.
- S3 업로드 완료 확인 후 서버가 실제 객체의 MIME 타입, 파일 시그니처, 용량과 이미지 가로 크기를 검증한다.
- 검증에 성공하면 `UPLOADED`로 변경한다.
- 일정 장소 또는 게시글에 연결되면 `LINKED`로 변경한다.
- 24시간 동안 연결되지 않은 `PENDING`, `UPLOADED` 파일은 배치로 삭제한다.
- MVP에서는 서버 측 이미지 리사이징, 원본 보관과 별도 파생본 생성 파이프라인을 구현하지 않는다.
- CDN을 사용하더라도 이미 생성된 WebP 파일의 전송 캐시 역할만 담당하며 이미지 변환 규칙은 프론트엔드가 수행한다.

#### 입력 / Request DTO

- `fileName: String`
- `contentType: image/webp`
- `sizeBytes: Long` — 최대 500KB
- `width: Int` — 최대 1080px
- `height: Int`
- `purpose: POST | TRIP_PLACE`

#### 출력 / Response DTO

- `fileId: Long`
- `uploadUrl: String`
- `objectKey: String`
- `expiresAt: LocalDateTime`

#### 예외사항 및 검증 로직

- 최종 업로드 파일이 WebP가 아니면 `400 UNSUPPORTED_FILE_TYPE`이다.
- 최종 업로드 파일이 500KB를 초과하면 `413 FILE_TOO_LARGE`를 반환한다.
- 가로 크기가 1080px를 초과하면 `422 IMAGE_DIMENSION_LIMIT_EXCEEDED`를 반환한다.
- S3 객체의 실제 정보가 발급 요청 메타정보와 다르면 `409 FILE_METADATA_MISMATCH`를 반환하고 해당 객체를 삭제한다.
- 목적별 첨부 개수 초과는 `422 IMAGE_LIMIT_EXCEEDED`다.
- 완료 API는 멱등하게 처리한다.

#### 연동 API 엔드포인트

- `POST /api/files/presigned-uploads`
- `POST /api/files/{fileId}/complete`

---

## 3.6 장소

### 3.6.1 기능명: 장소 검색

#### 기능 설명

- 로그인 사용자는 장소명 또는 지역명으로 국내 장소를 검색할 수 있다.
- 장소 검색은 홈 통합 검색, 일정 장소 추가, 일정 위시리스트와 저장 장소 기능에서 공통으로 사용한다.
- 외부 장소 검색 데이터는 네이버 API를 사용한다.
- 네이버 카테고리 원본값은 그대로 보존하고 여행 서비스용 대분류로 정규화한다.
- 국내 장소만 검색 대상으로 사용한다.
- MVP에서는 앱 전용 커스텀 대분류를 추가하지 않고 아래 고정 `PlaceCategory`만 사용한다.
  - `FOOD`
  - `CAFE`
  - `ATTRACTION`
  - `CULTURE_ART`
  - `ACTIVITY`
  - `NATURE`
  - `SHOPPING`
  - `ACCOMMODATION`
  - `OTHER`

#### 세부 로직 및 상태 변화

- JWT 인증을 필수로 검증한다.
- 검색어를 검증한 후 네이버 장소 검색 API를 호출한다.
- 네이버 응답을 `PlaceSummaryResponse`로 정규화한다.
  - 네이버 장소 식별값 → `externalPlaceId`
  - `title` → `name`
  - `category` → `externalCategory`
  - `roadAddress`가 있으면 우선 사용하고 없으면 `address` → `address`
  - `mapx`, `mapy` → `longitude`, `latitude`
- 장소명의 HTML 태그를 제거한다.
- 원본 카테고리 전체 문자열은 `externalCategory`에 보존한다.
- 여행 서비스용 대분류는 `category: PlaceCategory`로 별도 반환한다.
- 여행과 관련성이 낮은 병원, 학원, 행정기관, 일반 사무시설 등의 장소는 검색 결과에서 제외한다.
- 여행 관련 장소이지만 고정 대분류에 명확하게 매핑되지 않으면 `OTHER`로 반환한다.
- MVP에서는 `PHOTO_SPOT`, `NIGHT_VIEW`, `BEACH`, `MARKET` 등의 새 대분류를 추가하지 않는다. 이러한 특성은 필요 시 추후 태그 체계로 확장한다.
- 네이버 원본 카테고리 문자열의 각 depth를 정규화한 뒤 특정 키워드가 포함되어 있는지 `Contains` 방식으로 검사하여 고정 대분류에 매핑한다.
- 매핑 조건은 애플리케이션의 분산된 `if/else` 코드에 하드코딩하지 않고 `place_category_mapping_rules` 테이블에서 우선순위, 포함 키워드, 대상 카테고리와 제외 여부를 관리한다.
- 한 장소가 여러 규칙에 일치할 수 있으므로 `priority ASC` 기준으로 가장 먼저 일치한 규칙 하나를 적용한다.
- MVP 카테고리 키워드 규칙은 다음과 같다.
  1. `카페`, `다방`, `베이커리` 포함 → `CAFE`
  2. `음식점` 포함이며 위 CAFE 규칙에 해당하지 않음 → `FOOD`
  3. `숙박`, `펜션`, `호텔`, `모텔` 포함 → `ACCOMMODATION`
  4. `문화`, `예술`, `전시`, `박물관` 포함 → `CULTURE_ART`
  5. `스포츠`, `레저`, `액티비티` 포함 → `ACTIVITY`
  6. `쇼핑`, `마트`, `시장` 포함 → `SHOPPING`
  7. `자연`, `산`, `바다`, `계곡` 포함 → `NATURE`
  8. `관광`, `명소`, `공원` 포함 → `ATTRACTION`
- 여행 관련 장소지만 위 규칙에 매핑되지 않으면 `OTHER`로 반환한다.
- 병원, 행정기관, 학원, 일반 사무시설 등 여행과 직접 관련 없는 키워드가 포함된 장소는 `OTHER`로 노출하지 않고 검색 결과에서 제외한다.
- 네이버 검색 결과의 `externalPlaceId`가 `place_external_ids(provider=NAVER, externalPlaceId)`에 이미 매핑되어 있으면 연결된 canonical `placeId`를 반환하고 사용자의 저장 여부를 `saved`로 반환한다.
- 검색만으로 장소를 DB에 저장하지 않는다.
- 외부 검색 결과의 `placeId`가 `null`이면 저장·위시리스트·Day 추가 등의 액션 직전에 `POST /api/places/resolve`로 내부 `placeId`를 먼저 확보한다.
- `/resolve`는 먼저 `place_external_ids(provider=NAVER, externalPlaceId)`를 조회한다. 기존 매핑이 있으면 연결된 `placeId`를 반환한다. 매핑이 없으면 기존 canonical 장소와의 고신뢰 동일 장소 후보를 확인하고, 정확히 하나의 후보만 확정되면 해당 `placeId`에 NAVER 외부 ID를 연결한다. 그 외에는 새 `places`와 `placeId`를 생성한 뒤 NAVER 외부 ID를 연결한다.
- 외부 식별자의 중복 방지는 `places`가 아니라 `place_external_ids(provider, externalPlaceId)` Unique 제약으로 처리한다.
- 네이버 무료 일일 호출 한도를 초과하면 유료 과금으로 전환하지 않고 당일 검색을 차단한다.

#### 입력 / Request DTO

- `query: String`
- `latitude: BigDecimal?`
- `longitude: BigDecimal?`
- `category: PlaceCategory?`
- `start: Int = 1`
- `display: Int = 15`

#### 출력 / Response DTO

- `items`
  - `placeId: Long?`
  - `externalPlaceId: String`
  - `name: String`
  - `externalCategory: String`
  - `category: PlaceCategory`
  - `address: String`
  - `latitude: BigDecimal`
  - `longitude: BigDecimal`
  - `saved: Boolean`
- `total: Int`
- `isEnd: Boolean`

#### 예외사항 및 검증 로직

- 비로그인 요청은 `401 UNAUTHORIZED`다.
- 검색어가 비어 있으면 `400 SEARCH_QUERY_REQUIRED`다.
- 검색어가 2자 미만이면 `400 SEARCH_QUERY_TOO_SHORT`다.
- 고정 대분류에 없는 값은 `400 INVALID_PLACE_CATEGORY`다.
- 잘못된 좌표는 `400 INVALID_COORDINATES`다.
- 네이버 API 장애는 `503 PLACE_SEARCH_UNAVAILABLE`이다.
- 일일 무료 호출 한도 초과는 `503 PLACE_SEARCH_DAILY_LIMIT_EXCEEDED`이며 사용자 메시지는 `금일 장소 검색 한도가 초과되었습니다.`로 반환한다.

#### 연동 API 엔드포인트

- `GET /api/places/search`

---

### 3.6.2 기능명: 장소 상세 조회

#### 기능 설명

- 로그인 사용자는 장소명, 주소, 카테고리와 좌표를 조회한다.
- 사용자의 장소 저장 여부를 함께 반환한다.
- 내부에 존재하지만 폐업 또는 비활성으로 확인된 장소도 삭제된 장소처럼 숨기지 않고 `200 OK + status=CLOSED`로 반환한다.

#### 세부 로직 및 상태 변화

- JWT 인증을 필수로 검증한다.
- 내부 DB 장소정보를 우선 조회한다.
- 필요할 때 네이버 API의 최신 정보로 보완한다.
- `externalCategory`와 정규화된 `category`를 함께 반환한다.
- 저장 관계를 조회하여 `saved`를 반환한다.
- 외부 정보 또는 운영 데이터에서 폐업이 확인되면 `places.status=CLOSED`로 갱신한다.
- `CLOSED` 장소는 기존 장소명, 주소, 좌표와 저장 여부를 반환하되 응답 상태 필드로 폐업 사실을 명확히 전달한다.

#### 출력 / Response DTO

- `placeId: Long`
- `status: ACTIVE | CLOSED`
- `name: String`
- `externalCategory: String`
- `category: PlaceCategory`
- `address: String`
- `region: RegionSummary`
- `latitude: BigDecimal`
- `longitude: BigDecimal`
- `imageUrl: String?`
- `saved: Boolean`

#### 예외사항 및 검증 로직

- 비로그인 요청은 `401 UNAUTHORIZED`다.
- 내부 DB에 존재하지 않는 장소는 `404 PLACE_NOT_FOUND`다.
- 폐업·비활성 장소는 오류로 처리하지 않고 `200 OK`와 `status=CLOSED`를 반환한다.

#### 연동 API 엔드포인트

- `GET /api/places/{placeId}`

---

### 3.6.3 기능명: 장소 저장·취소·목록·메모 관리

#### 기능 설명

- 사용자는 장소를 저장하거나 저장 취소할 수 있다.
- 저장 장소는 폴더·지역 분류 없이 최신 저장순 일렬 목록으로 제공한다.
- 저장 시점에는 메모를 입력받지 않고 저장 이후 별도로 작성·수정한다.

#### 세부 로직 및 상태 변화

- 외부 검색 결과가 아직 내부 DB에 없고 `placeId=null`이면 저장 요청 전에 `/api/places/resolve`를 호출하여 내부 `placeId`를 확보한다.
- `(userId, placeId)` 저장 관계 중복을 방지한다.
- 저장·취소 요청은 멱등하게 처리한다.
- 목록은 저장일 내림차순과 커서 페이지네이션을 사용한다.
- 저장 장소 메모는 사용자 본인만 조회·수정할 수 있다.
- 메모는 공백을 포함하여 최대 100자다.
- 저장 관계를 취소하면 해당 개인 메모도 함께 삭제한다.

#### 입력 / Request DTO

- 저장·취소: Path `placeId: Long`
- 목록: `cursor: Long?`, `size: Int = 20` — 최대 50
- 메모 수정: `memo: String?`

#### 출력 / Response DTO

- 저장·취소 성공: `204 No Content`
- 목록: `SavedPlaceListResponse`
  - `items: List<SavedPlaceItem>` — required, 결과가 없으면 `[]`
    - `placeId: Long`
    - `name: String`
    - `category: PlaceCategory`
    - `thumbnailUrl: String?`
    - `memo: String?`
    - `savedAt: LocalDateTime`
  - `nextCursor: Long?` — required+nullable
- 메모 수정 성공: `placeId`, `memo`, `updatedAt`

#### 예외사항 및 검증 로직

- 비로그인 요청은 `401 UNAUTHORIZED`다.
- 존재하지 않는 장소는 `404 PLACE_NOT_FOUND`다.
- 메모가 100자를 초과하면 `400 SAVED_PLACE_MEMO_TOO_LONG`이다.

#### 연동 API 엔드포인트

- `PUT /api/users/me/saved-places/{placeId}`
- `DELETE /api/users/me/saved-places/{placeId}`
- `GET /api/users/me/saved-places`
- `PATCH /api/users/me/saved-places/{placeId}/memo`

---

### 3.6.4 기능명: 외부 장소 내부 ID 확정

#### 기능 설명

- 외부 장소 검색 결과의 `placeId`가 `null`일 때 저장·위시리스트·Day 추가 등 내부 ID가 필요한 액션 직전에 내부 `placeId`를 확정한다.
- 검색 결과 조회만으로 내부 장소를 생성하지 않는다.
- 이미 같은 외부 장소가 내부 DB에 존재하면 새 장소를 만들지 않고 기존 `placeId`를 반환한다.

#### 세부 로직 및 상태 변화

- 이 공개 `/resolve`는 네이버 외부 검색 결과를 내부 장소로 확정하는 용도이므로 Request DTO에 `provider`를 받지 않고 Backend 내부에서 `provider=NAVER`로 고정한다.
- `place_external_ids(provider=NAVER, externalPlaceId)`를 먼저 조회한다. 기존 매핑이 있으면 연결된 canonical `placeId`를 즉시 반환한다.
- 기존 NAVER 매핑이 없으면 전달받은 장소명·주소·좌표를 정규화하여 기존 `places`의 동일 장소 후보를 확인한다.
- 자동 연결은 **정규화 장소명 일치 AND 정규화 주소 일치 AND 좌표 거리 50m 이내**를 모두 만족하고 고신뢰 후보가 정확히 1개일 때만 허용한다. 장소명은 HTML 제거·trim·연속 공백 축소 후 비교하고, 주소는 trim·연속 공백 축소 후 비교한다.
- 고신뢰 후보가 정확히 1개면 새 `places`를 생성하지 않고 해당 `placeId`에 `place_external_ids(provider=NAVER, externalPlaceId)`를 추가한다.
- 후보가 없거나 복수이거나 조건이 불명확하면 잘못된 병합(False Positive)을 피하기 위해 새 `places`를 생성하여 새로운 `placeId`를 발급하고 NAVER 외부 ID를 연결한다. MVP에서는 애매한 후보를 자동 병합하지 않는다.
- `place_external_ids(provider, externalPlaceId)` Unique 제약과 트랜잭션으로 동일 외부 ID의 동시 중복 연결을 방지한다.
- 기존 장소 반환, 기존 canonical 장소에 외부 ID 연결, 신규 장소 생성 모두 같은 resolve 계약을 사용하므로 성공 응답은 `200 OK`로 통일한다.

#### 입력 / Request DTO

- `externalPlaceId: String`
- `placeName: String`
- `address: String`
- `latitude: BigDecimal`
- `longitude: BigDecimal`

#### 출력 / Response DTO

- `placeId: Long`

#### 연동 API 엔드포인트

- `POST /api/places/resolve`

---

### 3.6.5 기능명: 관광지 초기 데이터 적재 및 외부 식별자 매핑

#### 기능 설명

- AI/관광데이터 파트가 한국관광공사 TourAPI 데이터를 사전에 수집·정규화하여 Backend 초기 적재용 CSV 또는 JSON으로 전달한다.
- Backend는 별도의 공개 Bulk API를 만들거나 `/api/places/resolve`를 대량 반복 호출하지 않고 초기 Seeder/import script 또는 일회성 batch로 적재한다.
- 실제 서비스 내부 장소는 `places`, 외부 Provider 식별자는 `place_external_ids`에 분리 저장한다.
- 초기 적재 후 Backend가 확정한 `provider / externalPlaceId / placeId` 매핑을 AI/관광데이터 파트에 다시 전달하며 AI 추천 데이터는 이 내부 `placeId`를 기준으로 사용한다.

#### 세부 로직 및 상태 변화

- TourAPI 장소는 `provider=KTO_TOUR_API`, `externalPlaceId=contentId`로 해석한다. `contentId`를 `places.placeId`로 직접 사용하지 않는다.
- importer는 먼저 `place_external_ids(provider=KTO_TOUR_API, externalPlaceId=contentId)`를 조회하여 이미 적재된 데이터면 같은 canonical `placeId`를 재사용하므로 재실행 가능하게 구성한다.
- 외부 ID 매핑이 없고 기존 canonical 장소 후보가 존재하는 경우에도 `3.6.4`와 동일한 보수적 기준인 **정규화 장소명 일치 AND 정규화 주소 일치 AND 좌표 거리 50m 이내**, 고신뢰 후보 정확히 1개 조건을 모두 만족할 때만 기존 `placeId`에 연결한다.
- 위 조건을 만족하지 않거나 후보가 복수이면 새 `places`와 `placeId`를 생성한다. 잘못된 병합 방지가 중복 제거보다 우선한다.
- `place_external_ids(provider, externalPlaceId)`는 Unique이며 하나의 canonical `placeId`에는 `KTO_TOUR_API`, `NAVER` 등 서로 다른 Provider의 외부 ID를 연결할 수 있다.
- AI/관광데이터 파트는 초기 데이터의 장소명·주소·좌표 정규화와 동일 장소 후보 추출을 지원할 수 있으나 최종 병합 여부, `placeId` 생성, 외부 ID 연결의 최종 책임은 Backend에 있다.
- 추천 시 TourAPI를 실시간 호출하지 않는다.

#### 초기 적재 데이터 최소 항목

- `provider: KTO_TOUR_API`
- `externalPlaceId: String` — TourAPI `contentId`
- `name: String`
- `address: String`
- `latitude: BigDecimal`
- `longitude: BigDecimal`
- `regionCode: String` — 5자리 시군구 코드
- `category: PlaceCategory` — Backend 공통 카테고리로 정규화 가능한 값

#### Backend → AI 초기 매핑 결과

- 최소 `provider / externalPlaceId / placeId`를 전달한다.
- AI/관광데이터 파트는 이 매핑을 추천용 사전 정규화 데이터에 반영하고 이후 Recommendation Request/Callback에서는 외부 ID가 아니라 Backend 내부 `placeId`만 사용한다.

---

## 3.7 여행 일정

### 3.7.1 기능명: 직접 일정 생성

#### 기능 설명

- 사용자는 단일 지역, 날짜, 단일 동행 유형, 최대 3개 테마와 일정 밀도를 선택한다.
- 제목은 시스템이 자동 생성한다.
- 직접 일정은 Day별 장소가 없는 빈 일정으로 생성된다.
- 직접 일정 생성 입력 흐름은 `지역 → 날짜 → 동행 유형 → 여행 테마 → 일정 밀도 → 일정 생성` 순서이며 생성 후 Day별 장소를 사용자가 직접 추가한다.
- 여행 기간 최대 일수 제한은 두지 않는다.
- 완료된 여행도 수정할 수 있다.
- 다만 발행된 Post가 연결된 Trip은 Post가 삭제될 때까지 경로 구조 수정이 잠기며, 경로와 무관한 정보만 수정할 수 있다.

#### 세부 로직 및 상태 변화

- Trip과 여행 날짜 수만큼의 TripDay를 하나의 트랜잭션으로 생성한다.
- `sourceType=MANUAL`로 저장한다.
- 공개 범위 기본값은 `PRIVATE`다.
- 일정 진행 상태는 날짜를 기준으로 계산한다.
  - `UPCOMING`: 현재 날짜가 `startDate`보다 이전
  - `IN_PROGRESS`: `startDate ≤ 현재 날짜 ≤ endDate`
  - `COMPLETED`: 현재 날짜가 `endDate`보다 이후
- 취소 여부는 별도 상태값을 저장하지 않고 `cancelledAt: LocalDateTime?`으로 관리한다.
- `cancelledAt`이 있으면 날짜 판정 결과보다 우선하여 유효 상태를 `CANCELLED`로 반환한다.
- `companionType`은 `SOLO | COUPLE | FRIENDS | FAMILY | OTHER` 중 하나의 단일 Enum으로 저장한다.
- `themes`는 `ACTIVITY | SNS_HOTPLACE | NATURE | ATTRACTION | SHOPPING | FOOD` 중 최소 1개, 최대 3개를 중복 없이 저장한다.
  - `ACTIVITY` → 액티비티
  - `SNS_HOTPLACE` → SNS 핫플
  - `NATURE` → 자연과 함께
  - `ATTRACTION` → 유명 관광지
  - `SHOPPING` → 쇼핑
  - `FOOD` → 먹방
- 각 Day의 `dayNumber`는 1부터 순차 부여한다.
- 신규 Trip에는 발행 Post가 없으므로 `routeEditable=true`다.

#### 입력 / Request DTO

- `regionCode: String` — 5자리 시군구 코드 한 개
- `startDate: LocalDate`
- `endDate: LocalDate`
- `companionType: CompanionType`
- `themes: Set<TravelTheme>`
- `pace: RELAXED | NORMAL | DENSE`

#### 출력 / Response DTO

- 성공 시 `201 Created`
- `tripId`, `title`, `sourceType`
- `status: UPCOMING | IN_PROGRESS | COMPLETED | CANCELLED`
- `cancelledAt: LocalDateTime?`
- `visibility: PRIVATE`
- `region: RegionSummary`, `startDate`, `endDate`
- `companionType`, `themes`, `pace`, `hashtags`, `days`
- `routeEditable: true`
- `routeLockedReason: null`

#### 예외사항 및 검증 로직

- 날짜 누락은 `400 TRIP_DATE_REQUIRED`다.
- 종료일이 시작일보다 빠르면 `400 INVALID_TRIP_PERIOD`다.
- 지역은 5자리 시군구 코드 한 개를 필수로 전달하며 누락 시 `400 REGION_REQUIRED`를 반환한다.
- 동행 유형 오류는 `400 INVALID_COMPANION_TYPE`이다.
- 테마 누락·3개 초과는 `400 THEME_REQUIRED`, `400 TOO_MANY_THEMES`다.
- 일정 밀도 오류는 `400 INVALID_TRIP_PACE`다.

#### 연동 API 엔드포인트

- `POST /api/trips`

---

### 3.7.2 기능명: 일정 상세 조회

#### 기능 설명

- 일정 기본 정보와 Day별 장소 목록을 조회한다.
- 지도에는 전체 또는 선택 Day의 번호 핀과 단순 연결선을 표시한다.
- 공개 범위는 `PUBLIC`, `MEMO_PRIVATE`, `PRIVATE`다.
- 소유자는 모든 내용을 조회할 수 있다.
- 비로그인 사용자도 `PUBLIC`, `MEMO_PRIVATE` 일정을 조회할 수 있다.
- 미확정 AI 결과는 `previewId` 기반 미리보기 API에서 조회한다.
- 일정 소유자에게 현재 Trip의 경로 수정 가능 여부와 잠금 원인이 함께 제공된다.

#### 세부 로직 및 상태 변화

- `PUBLIC`은 일정, 장소, 메모와 사진을 모두 반환한다.
- `MEMO_PRIVATE`은 일정, 장소와 사진을 반환하고 장소 메모만 숨긴다.
- 비소유자에게 `memo=null`, `memoMasked=true`를 반환한다.
- `PRIVATE`은 소유자만 조회할 수 있으며 비소유자에게 내용을 반환하지 않는다.
- 일정 상태는 조회 시 날짜 기준으로 계산한다.
- 다만 `cancelledAt`이 존재하면 날짜와 무관하게 `status=CANCELLED`로 반환한다.
- 취소된 Trip은 읽기 전용이므로 소유자 응답에도 `routeEditable=false`, `contentEditable=false`, `routeLockedReason=TRIP_CANCELLED`를 반환한다.
- 공개 범위 기본값은 `PRIVATE`다.
- AI 작업 상태는 Trip 상태와 구분한다.
- 이동 경로는 장소 좌표와 순서를 이용한 단순 직선·점선 연결로 표시한다.
- `publishedAt IS NOT NULL`이고 `deletedAt IS NULL`인 Post가 Trip에 연결되어 있으면 경로 잠금 상태로 판단한다.
- 발행 Post의 공개 범위가 `PUBLIC`, `MEMO_PRIVATE`, `PRIVATE` 중 무엇이든 잠금을 유지하며, 운영자가 `BLOCKED`로 변경해도 Post가 삭제되지 않은 동안 잠금을 유지한다.
- DRAFT Post만 존재하면 경로 잠금을 적용하지 않는다.

#### 출력 / Response DTO

- `tripId`, `owner`, `sourceType`, `status`, `visibility`
- `cancelledAt: LocalDateTime?`
- `title`, `summary`, `region: RegionSummary`, `startDate`, `endDate`
- `pace`, `companionType`, `themes`, `hashtags`
- `routeEditable: Boolean`
- `routeLockedReason: PUBLISHED_POST_EXISTS | TRIP_CANCELLED | null`
- `publishedPostId: Long?`
- `contentEditable: Boolean`
- `days`
  - `tripPlaceId`, `placeId`, `order`
  - `memo`, `memoMasked`, `images: List<ImageSummary>`, `reason`, `coordinates`
- 비소유자 응답에서는 편집 기능이 없으므로 `routeEditable=false`, `contentEditable=false`를 반환하고 잠금 원인과 연결 Post ID는 노출하지 않는다.
- 프론트엔드는 `routeEditable`, `contentEditable`, `routeLockedReason`을 서버 계약값으로 그대로 사용하며 날짜나 Post 상태를 기준으로 편집 가능 여부를 다시 계산하지 않는다.

#### 예외사항 및 검증 로직

- 존재하지 않는 일정은 `404 TRIP_NOT_FOUND`다.
- `PRIVATE` 일정의 비소유자·비로그인 접근은 `404 TRIP_NOT_FOUND`다.
- `MEMO_PRIVATE` 비소유자 응답에서는 메모를 반환하지 않는다.

#### 연동 API 엔드포인트

- `GET /api/trips/{tripId}`

---

### 3.7.3 기능명: 일정 기본 정보 수정

#### 기능 설명

- 일정 소유자는 예정·진행·완료 여행을 수정할 수 있다.
- 발행된 Post가 연결되지 않은 Trip은 경로 정보와 비경로 정보를 모두 수정할 수 있다.
- 발행된 Post가 연결된 Trip은 제목, 설명, 동행 유형, 테마, 일정 밀도와 Trip 공개 범위만 수정할 수 있고 지역·기간·Day 구조는 수정할 수 없다.

#### 세부 로직 및 상태 변화

- 경로 수정 필드는 `regionCode`, `startDate`, `endDate`다.
- 비경로 수정 필드는 `title`, `summary`, `companionType`, `themes`, `pace`, `visibility`다.
- `companionType`은 생성·조회·수정에서 단일값으로 통일한다.
- 경로 변경 요청 전에 `publishedAt IS NOT NULL AND deletedAt IS NULL`인 연결 Post 존재 여부를 Service 계층에서 검증한다.
- 경로가 잠긴 상태에서 경로 수정 필드가 하나라도 전달되면 전체 요청을 거부하고 비경로 필드도 함께 변경하지 않는다.
- 경로가 잠기지 않은 경우 날짜 증가 시 Day를 추가한다.
- 날짜 축소로 제거될 Day에 장소가 있으면 사용자 확인 없이 삭제하지 않는다.
- 확인되지 않은 요청에는 `409 TRIP_DAY_REMOVAL_CONFIRMATION_REQUIRED`와 제거 예정 Day·장소 요약을 반환하고, 사용자가 `confirmDayRemoval=true`로 재요청하면 삭제한다.
- 제거되는 Day의 장소 메모와 사진도 함께 삭제한다.
- `@Version` 낙관적 락으로 동시 수정 충돌을 방지한다.
- 수정 후 진행 상태는 변경된 날짜를 기준으로 다시 계산한다.

#### 입력 / Request DTO

- PATCH 일반 수정 필드는 optional이며, `version`만 동시성 제어를 위해 required다.
- `title: String` — optional, 전달 시 non-null
- `summary: String?` — optional, explicit `null`이면 설명 삭제
- `regionCode: String` — optional, 전달 시 5자리 시군구 코드 한 개이며 non-null
- `startDate: LocalDate` — optional, 전달 시 non-null
- `endDate: LocalDate` — optional, 전달 시 non-null
- `companionType: CompanionType` — optional, 전달 시 non-null
- `themes: Set<TravelTheme>` — optional, 전달 시 non-null이며 기존 1~3개 도메인 검증을 따른다.
- `pace: RELAXED | NORMAL | DENSE` — optional, 전달 시 non-null
- `visibility: PUBLIC | MEMO_PRIVATE | PRIVATE` — optional, 전달 시 non-null
- `confirmDayRemoval: Boolean = false` — optional, 전달 시 non-null
- `version: Long` — required

#### 예외사항 및 검증 로직

- 비소유자는 `403 TRIP_ACCESS_DENIED`다.
- 버전 충돌은 `409 TRIP_MODIFICATION_CONFLICT`다.
- 잠긴 Trip의 지역·기간·Day 구조 변경은 `409 TRIP_ROUTE_LOCKED_BY_PUBLISHED_POST`다.
- 완료된 일정이라는 이유만으로 수정 요청을 거부하지 않는다.
- `cancelledAt`이 존재하는 Trip은 제목, 날짜, 장소, 경로, 메모, 사진, 공개 범위 등 모든 수정 요청을 거부하고 읽기 전용으로 유지한다.
- 취소된 Trip 수정 요청은 `409 TRIP_CANCELLED_READ_ONLY`를 반환한다.

#### 연동 API 엔드포인트

- `PATCH /api/trips/{tripId}`

---

### 3.7.4 기능명: Day별 장소 추가·삭제

#### 기능 설명

- 일정 소유자는 특정 Day에 장소를 추가하거나 삭제한다.
- 발행된 Post가 연결된 Trip에서는 장소 추가·삭제를 허용하지 않는다.

#### 세부 로직 및 상태 변화

- 일정 소유권과 Day 소속을 검증한다.
- `cancelledAt`이 존재하면 장소 추가·삭제 요청을 `409 TRIP_CANCELLED_READ_ONLY`로 차단한다.
- 장소 추가·삭제 전에 Trip의 경로 잠금 여부를 검증한다.
- 동일한 `placeId`는 하나의 Trip 전체에서 최대 한 번만 포함할 수 있다. 같은 장소를 같은 Day뿐 아니라 서로 다른 Day에도 중복 추가할 수 없다.
- 이미 Trip에 포함된 장소를 다른 Day로 이동하는 것은 허용하지만, 이동 후에도 해당 Trip 전체에 동일 `placeId`는 하나만 존재해야 한다.
- Day별 최대 장소 수는 15개다.
- 장소 추가 시 해당 장소가 Trip 위시리스트에 있으면 위시리스트에서 자동 제거한다.
- 따라서 같은 Trip에서 동일 장소는 위시리스트와 Day에 동시에 존재할 수 없다.
- 장소 삭제 시 연결된 메모, 사진 관계와 실제 이미지 파일을 삭제한다.
- 삭제 후 Day 내 순서를 연속되게 재정렬한다.

#### 입력 / Request DTO

- `placeId: Long`
- `order: Int?`

#### 출력 / Response DTO

- 장소 추가 성공: `204 No Content`
- 장소 삭제 성공: `204 No Content`

#### 예외사항 및 검증 로직

- 존재하지 않는 Day는 `404 TRIP_DAY_NOT_FOUND`다.
- 존재하지 않는 장소는 `404 PLACE_NOT_FOUND`다.
- Trip 전체에서 동일 `placeId`가 이미 다른 Day 또는 같은 Day에 존재하면 `409 PLACE_ALREADY_ADDED`다.
- 15개 초과는 `422 TRIP_DAY_PLACE_LIMIT_EXCEEDED`다.
- 경로 잠금 상태에서는 `409 TRIP_ROUTE_LOCKED_BY_PUBLISHED_POST`를 반환한다.
- 취소된 Trip은 `409 TRIP_CANCELLED_READ_ONLY`다.

#### 연동 API 엔드포인트

- `POST /api/trips/{tripId}/days/{dayNumber}/places`
- `DELETE /api/trips/{tripId}/days/{dayNumber}/places/{tripPlaceId}`

---

### 3.7.5 기능명: 장소 순서 변경

#### 기능 설명

- Day 안에서 장소 순서를 드래그 방식으로 변경한다.
- 발행된 Post가 연결된 Trip에서는 장소 순서를 변경할 수 없다.

#### 세부 로직 및 상태 변화

- 클라이언트는 Day의 전체 `tripPlaceId`를 원하는 순서대로 전송한다.
- `cancelledAt`이 존재하면 순서 변경 요청을 `409 TRIP_CANCELLED_READ_ONLY`로 차단한다.
- 순서 변경 전에 Trip 경로 잠금 여부를 검증한다.
- 누락, 중복과 다른 Day 소속 ID를 검증한다.
- 하나의 트랜잭션에서 순서를 변경한다.

#### 입력 / Request DTO

- `orderedTripPlaceIds: List<Long>`

#### 예외사항 및 검증 로직

- 누락·중복은 `400 INVALID_PLACE_ORDER_REQUEST`다.
- 다른 Day 장소는 `400 TRIP_PLACE_DAY_MISMATCH`다.
- 경로 잠금 상태에서는 `409 TRIP_ROUTE_LOCKED_BY_PUBLISHED_POST`를 반환한다.
- 취소된 Trip은 `409 TRIP_CANCELLED_READ_ONLY`다.

#### 연동 API 엔드포인트

- `PUT /api/trips/{tripId}/days/{dayNumber}/place-orders`

---

### 3.7.6 기능명: 장소별 메모·사진 수정

#### 기능 설명

- 일정 소유자는 각 장소의 한줄 메모와 사진을 추가·수정할 수 있다. MVP에서는 방문 시각과 체류시간을 관리하지 않는다.
- 이 기능은 장소 ID, Day와 방문 순서를 바꾸지 않으므로 Trip 경로가 잠겨 있어도 사용할 수 있다.

#### 세부 로직 및 상태 변화

- `cancelledAt`이 존재하면 메모·사진 수정 요청을 `409 TRIP_CANCELLED_READ_ONLY`로 차단한다.
- 메모는 공백 포함 최대 100자다.
- 장소별 사진은 최대 3장이다.
- 파일 소유권, 업로드 완료 상태와 목적을 검증한다.
- 기존 목록에서 제거된 이미지 파일은 즉시 삭제한다.
- 요청에서 `placeId`, `dayNumber`, `order`는 변경할 수 없다.

#### 입력 / Request DTO

- `memo: String?` — optional, explicit `null`이면 메모 삭제
- `imageFileIds: List<Long>` — optional, 전달 시 non-null이며 빈 배열 `[]`은 사진 전체 제거

#### 출력 / Response DTO

- 수정 성공: `204 No Content`

#### 예외사항 및 검증 로직

- 비소유자는 `403 TRIP_ACCESS_DENIED`다.
- 메모 초과는 `400 TRIP_PLACE_MEMO_TOO_LONG`이다.
- 사진 3장 초과는 `422 TRIP_PLACE_IMAGE_LIMIT_EXCEEDED`다.
- 파일 소유권 오류는 `403 FILE_ACCESS_DENIED`다.
- 취소된 Trip은 `409 TRIP_CANCELLED_READ_ONLY`다.

#### 연동 API 엔드포인트

- `PATCH /api/trips/{tripId}/places/{tripPlaceId}/content`

---

### 3.7.7 기능명: 일정 위시리스트

#### 기능 설명

- Trip에 종속된 위시리스트 장소를 관리한다.
- 위시리스트는 AI 경로 배치에 사용할 수 있다.
- 발행된 Post가 연결된 Trip에서도 위시리스트 자체의 추가·삭제·조회는 가능하지만, 위시리스트 장소를 Day에 배정하거나 AI 결과를 Trip에 적용하는 경로 변경은 허용하지 않는다.

#### 세부 로직 및 상태 변화

- `(tripId, placeId)` 중복을 방지한다.
- 일정 소유자만 수정한다.
- `cancelledAt`이 존재하면 위시리스트 조회만 허용하고 추가·삭제·Day 배정 요청은 `409 TRIP_CANCELLED_READ_ONLY`로 차단한다.
- 경로가 잠기지 않은 Trip에서 Day에 장소를 배정하면 동일 장소를 위시리스트에서 자동 제거한다.
- 위시리스트와 Day에 동일 장소가 동시에 존재할 수 없다.

#### 입력 / Request DTO

- 추가: `placeId: Long`
- 목록: `cursor: Long?`, `size: Int = 20` (최대 50)

#### 출력 / Response DTO

- 추가 성공: `204 No Content`
- 조회: `WishlistPlaceListResponse`
  - `items: List<WishlistPlaceItem>` — required, 결과가 없으면 `[]`
    - `placeId: Long`
    - `name: String`
    - `category: PlaceCategory`
    - `address: String`
    - `thumbnailUrl: String?`
    - `addedAt: LocalDateTime`
    - `description: String`
  - `nextCursor: Long?` — required+nullable, 마지막 페이지는 `null`
- 삭제 성공: `204 No Content`

#### 예외사항 및 검증 로직

- 비소유자는 `403 TRIP_ACCESS_DENIED`다.
- 중복 추가는 멱등 처리한다.
- 이미 Day에 배정된 장소를 위시리스트에 추가하면 `409 PLACE_ALREADY_ASSIGNED_TO_DAY`를 반환한다.
- 취소된 Trip의 위시리스트 변경은 `409 TRIP_CANCELLED_READ_ONLY`다.

#### 연동 API 엔드포인트

- `POST /api/trips/{tripId}/wishlist-places`
- `GET /api/trips/{tripId}/wishlist-places`
- `DELETE /api/trips/{tripId}/wishlist-places/{placeId}`

---

### 3.7.8 기능명: 내 여행 목록 및 캘린더

#### 기능 설명

- 내 여행을 예정·진행·완료로 구분하여 조회한다.
- 확정 저장된 Trip만 목록과 캘린더에 표시한다.
- `cancelledAt IS NOT NULL`인 여행은 상태가 `CANCELLED`이며 내 여행 목록과 캘린더에서 표시하지 않는다.

#### 세부 로직 및 상태 변화

- 상태 판정은 `Asia/Seoul` 현재 날짜와 여행 날짜를 기준으로 한다.
  - `UPCOMING`: 오늘 < 시작일
  - `IN_PROGRESS`: 시작일 ≤ 오늘 ≤ 종료일
  - `COMPLETED`: 종료일 < 오늘
- 종료일이 지나면 별도 버튼 없이 자동으로 완료로 판정한다.
- `CANCELLED` 일정은 목록과 캘린더에서 제외한다.
- `MANUAL`, `AI` Trip을 함께 조회한다.
- 미확정 preview와 AI job은 포함하지 않는다.
- 커서 페이지네이션을 적용한다.
- 캘린더 조회 범위는 `from <= tripDate <= to`로 양 끝 날짜를 모두 포함하며 날짜 판정은 `Asia/Seoul`을 사용한다.
- 여행 캘린더는 요청한 `from~to` 조회 범위 밖의 과거 여행을 임의로 추가 포함하지 않는다. 범위 밖의 완료 여행이 필요하면 `GET /api/users/me/trips?category=COMPLETED`를 별도로 조회한다.

#### 입력 / Request DTO

- 목록: `category: UPCOMING | IN_PROGRESS | COMPLETED`, `cursor: Long?`, `size: Int = 20` (최대 50)
- 캘린더: `from: LocalDate`, `to: LocalDate`

#### 출력 / Response DTO

- 목록: `TripListResponse`
  - `items: List<TripListItem>` — required, 결과가 없으면 `[]`
    - `tripId: Long`
    - `title: String`
    - `startDate: LocalDate`
    - `endDate: LocalDate`
    - `status: UPCOMING | IN_PROGRESS | COMPLETED`
    - `region: RegionSummary`
  - `nextCursor: Long?` — required+nullable
- 캘린더: `TravelCalendarResponse`
  - `items: List<TravelCalendarItem>` — required, 결과가 없으면 `[]`
    - `tripId: Long`
    - `title: String`
    - `startDate: LocalDate`
    - `endDate: LocalDate`
    - `status: UPCOMING | IN_PROGRESS | COMPLETED`
- `CANCELLED` Trip은 두 응답 모두에서 제외한다.

#### 예외사항 및 검증 로직

- 잘못된 기간은 `400 INVALID_DATE_RANGE`다.
- AI `jobId`, `previewId`는 내 여행 목록 식별자로 사용할 수 없다.

#### 연동 API 엔드포인트

- `GET /api/users/me/trips`
- `GET /api/users/me/travel-calendar`

---

### 3.7.9 기능명: 여행 취소

#### 기능 설명

- 일정의 진행 상태는 날짜로 계산하되, 사용자가 여행을 취소하면 `cancelledAt`에 취소 일시를 기록한다.
- `cancelledAt`이 존재하는 Trip은 여행 날짜와 관계없이 항상 `CANCELLED`로 판정한다.
- 취소는 상태를 되돌리는 임시 기능이 아니라 해당 Trip을 영구적인 읽기 전용 상태로 전환하는 기능이다.

#### 세부 로직 및 상태 변화

- JWT 사용자 ID와 Trip 소유자를 검증한다.
- 취소 요청 시 Trip 행을 잠그고 현재 `cancelledAt`과 연결 Post를 확인한다.
- `publishedAt IS NOT NULL AND deletedAt IS NULL`인 Post가 연결되어 있으면 Trip을 취소할 수 없다.
- 발행 Post가 연결된 경우 사용자가 먼저 해당 Post를 완전히 삭제해야 하며, `PRIVATE` 전환이나 운영 `BLOCKED` 처리는 취소 허용 조건이 아니다.
- 발행 Post가 없으면 `cancelledAt=현재시각`을 기록한다.
- 취소 후 날짜, 지역, Day, 장소, 순서, 위시리스트, 메모, 사진, 공개 범위, 제목과 기타 부가정보를 포함한 모든 수정 요청을 차단한다.
- 취소된 Trip은 내 여행 목록과 캘린더에서 제외하지만 소유자는 직접 상세 조회를 통해 읽기 전용으로 확인할 수 있다.
- 취소 해제 또는 복구 API는 제공하지 않는다.
- 이미 취소된 Trip에 대한 반복 취소 요청은 멱등하게 `204 No Content`를 반환한다.

#### 입력 / Request DTO

- Path `tripId: Long`
- 별도 Request Body 없음

#### 출력 / Response DTO

- 성공 시 `204 No Content`

#### 예외사항 및 검증 로직

- 비로그인 요청은 `401 UNAUTHORIZED`다.
- 존재하지 않는 Trip은 `404 TRIP_NOT_FOUND`다.
- 비소유자는 `403 TRIP_ACCESS_DENIED`다.
- 발행된 Post가 연결된 Trip은 `409 TRIP_CANCEL_REQUIRES_POST_DELETION`을 반환하고 공통 `ErrorResponse.details.postId`에 연결된 `postId`를 포함한다.
- 취소된 Trip의 수정 API 요청은 `409 TRIP_CANCELLED_READ_ONLY`다.

#### 연동 API 엔드포인트

- `POST /api/trips/{tripId}/cancel`

---

## 3.8 여행 기록

### 3.8.1 기능명: 여행 기록 작성 및 임시 저장

#### 기능 설명

- 사용자는 Trip을 기반으로 여행 게시글을 임시 저장하거나 발행한다.
- Trip과 Post는 별도 객체다.
- 하나의 Trip에는 삭제되지 않은 Post를 하나만 둘 수 있다.
- 본문은 일반 텍스트만 지원한다.
- 제목은 최대 50자, 본문은 최대 2,000자다.
- 해시태그는 게시글당 최대 5개이며 각 태그는 최대 10자다.
- DRAFT Post는 Trip 경로를 잠그지 않으며, Post가 최초 발행되어 `publishedAt`이 설정되는 순간부터 Trip 경로를 잠근다.

#### 세부 로직 및 상태 변화

- Post는 `tripId`를 참조한다.
- 임시 저장은 `status=DRAFT`, 발행은 `status=PUBLISHED`로 저장한다.
- 강제 숨김 시 운영자가 `status=BLOCKED`로 변경한다.
- `DRAFT`는 작성자만 조회할 수 있고 커뮤니티·검색·포토맵·통계에서 제외한다.
- 제목, 본문, 이미지, 장소, 해시태그와 공개 범위를 하나의 트랜잭션으로 저장한다.
- 게시글 이미지는 최대 10장이다.
- 제목, 본문과 해시태그 길이·개수 제한은 DRAFT와 발행 요청 모두에 적용한다.
- `publish=false`인 DRAFT 저장에서는 `title`, `content`가 비어 있거나 `null`이어도 허용한다. `publish=true`인 발행 요청에서만 제목과 본문을 필수로 검증한다.
- DB 컬럼은 `posts.title VARCHAR(50)`, `posts.content TEXT`, 해시태그명은 `VARCHAR(10)` 기준으로 설계한다.
- DRAFT에서 최초 발행할 때 Trip 행을 잠그고 `publishedAt`을 저장하여 Post 발행과 동시 Trip 경로 변경 경쟁 조건을 방지한다.
- 경로 잠금 판정 기준은 연결 Post의 `publishedAt IS NOT NULL AND deletedAt IS NULL`이다.
- 발행 후 `visibility`가 `PRIVATE`로 변경되거나 `status=BLOCKED`가 되어도 Post가 삭제되지 않은 동안 경로 잠금을 유지한다.
- 삭제된 Post와 같은 Trip으로 새 Post를 작성할 수 있으므로 단순 `UNIQUE(posts.trip_id)`를 사용하지 않는다.
- `[AI 제안·구현 기준]` MySQL에서는 `deletedAt IS NULL`인 Post만 유일하도록 생성 컬럼 `active_trip_id`와 유니크 인덱스를 사용하거나, Trip 행 잠금과 Service 검증으로 동일 조건을 보장한다.
- Trip 공개 범위는 일정 상세 접근을, Post 공개 범위는 커뮤니티 게시글 접근을 각각 제어하며 자동 동기화하지 않는다.
- 게시글 생성 API는 DRAFT/PUBLISHED 상태별로 분리하지 않고 `POST /api/posts` 하나를 사용하며 OpenAPI `oneOf`을 사용하지 않는다.
- `publish=false`이면 `title`, `content` 미완성을 허용하고, `publish=true`이면 두 필드를 서버에서 필수 검증한다.

#### 입력 / Request DTO

- `tripId: Long`
- `title: String?`
- `content: String?`
- `representativeFileId: Long?`
- `imageFileIds: List<Long>`
- `placeIds: List<Long>`
- `hashtags: List<String>`
- `visibility: PUBLIC | MEMO_PRIVATE | PRIVATE`
- `publish: Boolean`

#### 출력 / Response DTO

- 성공 시 `201 Created`
- `postId`
- `status: DRAFT | PUBLISHED`
- `visibility`
- `publishedAt`
- `createdAt`
- `tripRouteLocked: Boolean`

#### 예외사항 및 검증 로직

- 제목·본문 누락은 발행 요청에서 `400 INVALID_POST_CONTENT`다.
- 제목이 50자를 초과하면 `400 POST_TITLE_TOO_LONG`이다.
- 본문이 2,000자를 초과하면 `400 POST_CONTENT_TOO_LONG`이다.
- 해시태그가 5개를 초과하면 `400 POST_HASHTAG_LIMIT_EXCEEDED`, 개별 태그가 10자를 초과하면 `400 POST_HASHTAG_TOO_LONG`이다.
- 동일 Trip에 삭제되지 않은 Post가 있으면 `409 POST_ALREADY_EXISTS_FOR_TRIP`이다.
- 타인 Trip은 `403 TRIP_ACCESS_DENIED`다.
- 이미지 10장 초과는 `422 POST_IMAGE_LIMIT_EXCEEDED`다.
- Post 발행과 Trip 경로 수정이 충돌하면 한 요청만 성공하도록 트랜잭션 락을 적용한다.

#### 연동 API 엔드포인트

- `POST /api/posts`

---

### 3.8.2 기능명: 게시글 공개 범위 설정

#### 기능 설명

- `PUBLIC`은 경로, 장소, 장소 메모, 장소 사진과 본문을 공개한다.
- `MEMO_PRIVATE`은 게시글 본문과 경로·장소·사진을 공개하고 장소 메모만 숨긴다.
- `PRIVATE`은 작성자만 조회할 수 있으며 공유 링크로도 접근할 수 없다.
- `MEMO_PRIVATE` 게시글은 커뮤니티 목록에 노출한다.

#### 세부 로직 및 상태 변화

- 공개 범위는 Post의 `visibility`로 저장한다.
- 비작성자에게 `MEMO_PRIVATE` 장소 메모는 `null`, `memoMasked=true`로 반환한다.
- `contentMasked`는 사용하지 않는다.
- 장소 사진은 `MEMO_PRIVATE`에서도 공개한다.
- `PRIVATE` 게시글은 커뮤니티, 검색, 홈 추천에서 제외한다.

#### 예외사항 및 검증 로직

- `PRIVATE` 비작성자 접근은 `404 POST_NOT_FOUND`다.
- `BLOCKED` 게시글 접근도 `404 POST_NOT_FOUND`다.

---

### 3.8.3 기능명: 게시글 목록·상세 조회

#### 기능 설명

- 작성자는 자신의 `DRAFT | PUBLISHED | BLOCKED` 게시글 목록을 조회한다.
- 비로그인 사용자는 `PUBLIC`, `MEMO_PRIVATE`로 발행된 게시글 상세를 조회할 수 있다.

#### 세부 로직 및 상태 변화

- 내 목록에는 공개 범위와 관계없이 삭제되지 않은 자신의 `DRAFT | PUBLISHED | BLOCKED` 게시글을 포함한다.
- 마이페이지 기록 수에서는 DRAFT를 제외한다.
- 일반 상세는 `status=PUBLISHED`이며 `visibility IN (PUBLIC, MEMO_PRIVATE)`인 게시글만 비작성자에게 허용한다.
- `status=BLOCKED` 또는 삭제된 게시글은 일반 사용자에게 반환하지 않는다.
- 상세 조회와 조회수 집계는 분리한다.

#### 입력 / Request DTO

- 내 목록: `cursor: Long?`, `size: Int = 20` (최대 50)

#### 출력 / Response DTO

- 목록: `MyPostsResponse`
  - `items: List<MyPostItem>` — required, 결과가 없으면 `[]`
    - `postId: Long`
    - `tripId: Long`
    - `title: String?` — Key는 required이며 DRAFT에서 값은 `null` 가능
    - `thumbnailUrl: String?`
    - `region: RegionSummary`
    - `status: DRAFT | PUBLISHED | BLOCKED`
    - `visibility: PUBLIC | MEMO_PRIVATE | PRIVATE`
    - `createdAt: LocalDateTime`
  - `nextCursor: Long?` — required+nullable
- 상세: `PostDetailResponse`
  - 작성자 정보 — `birdType` 포함
  - `title`
  - `content`
  - `images: List<ImageSummary>`
  - `places: List<PostDetailPlaceResponse>`
    - `placeId: Long`
    - `name: String`
    - `category: PlaceCategory`
    - `address: String`
    - `latitude: BigDecimal`
    - `longitude: BigDecimal`
    - `dayNumber: Int`
    - `order: Int`
    - `memo: String?` — Key는 required이며 값은 nullable
    - `memoMasked: Boolean`
    - `images: List<ImageSummary>` — required·non-null, 없으면 `[]`
  - `route: List<PostDetailRouteDay>`
    - `dayNumber: Int`
    - `routePoints: List<Coordinates>` — 해당 Day 장소의 방문 순서대로 정렬된 좌표 목록
      - `latitude: BigDecimal`
      - `longitude: BigDecimal`
  - `visibility`
  - `viewCount`
  - `saveCount`
  - `shareCount`
- `PostDetailResponse`의 top-level에는 `region`, `companionType`, `themes`, `category`, `address`, `latitude`, `longitude`, `publishedAt`, `createdAt`을 포함하지 않는다. 다만 장소별 `category`, `address`, `latitude`, `longitude`는 `places` 내부의 표준 장소 DTO 필드로 반환한다.
- `route`는 네이버 Directions 결과가 아니라 지도·경로 표시 정책과 동일하게 Day별 장소의 `latitude`, `longitude`를 방문 순서대로 반환한다.
- `MEMO_PRIVATE` 비작성자 상세에서는 장소 메모를 `memo=null`, `memoMasked=true`로 반환하고 장소 사진은 공개한다.

#### 예외사항 및 검증 로직

- 존재하지 않거나 접근 불가능하면 `404 POST_NOT_FOUND`다.
- 비로그인 사용자가 DRAFT·PRIVATE·BLOCKED 게시글을 요청하면 `404`다.

#### 연동 API 엔드포인트

- `GET /api/users/me/posts`
- `GET /api/posts/{postId}`

---

### 3.8.4 기능명: 여행 기록 수정·삭제

#### 기능 설명

- 작성자만 게시글을 수정·삭제할 수 있다.
- 완료 여행에 연결된 게시글도 수정할 수 있다.
- 발행 이후에도 제목 오타, 본문, 대표 이미지, 첨부 이미지, 공개 범위와 해시태그는 수정할 수 있다.
- 발행 이후에는 Post의 연결 Trip, 게시 경로 장소 목록과 방문 순서를 변경할 수 없다.

#### 세부 로직 및 상태 변화

- DRAFT Post는 제목, 본문, 이미지, 장소, 해시태그와 공개 범위를 수정할 수 있다.
- PUBLISHED 또는 BLOCKED Post는 제목, 본문, 대표 이미지, 첨부 이미지, 해시태그와 공개 범위만 수정할 수 있다.
- 발행 Post의 `placeIds`, 연결 `tripId`, Day·장소 순서를 변경하는 요청은 거부한다.
- 공개 범위를 `PRIVATE`로 변경해도 원본 Trip 경로 잠금은 해제하지 않는다.
- 운영자가 `BLOCKED`로 변경해도 원본 Trip 경로 잠금은 해제하지 않는다.
- 삭제 시 연결된 S3 이미지 파일과 DB 파일 메타데이터를 즉시 삭제한다.
- 사용자 관점에서는 Post가 완전히 삭제되며 커뮤니티, 검색, 홈 추천과 포토맵에서 즉시 제외된다.
- 포토맵 방문 정보는 해당 게시글 삭제 즉시 재집계된다.
- 타 사용자의 저장 관계는 유지하되 `sourceAvailable=false`로 변경하고 원본 내용을 노출하지 않는다.
- 삭제 완료 시 `deletedAt`을 기록하여 원본 Trip의 경로 잠금을 해제한다.
- 이후 같은 Trip을 수정하여 새 Post를 발행해도 과거 삭제 Post를 참조한 SavedRoute는 새 Post에 자동 연결하지 않는다.
- 참조 무결성과 과거 SavedRoute 식별을 위해 삭제 Post 행은 tombstone 형태로 남길 수 있으며, 제목·본문·이미지 등 사용자 콘텐츠는 제거한다.

#### 입력 / Request DTO

- PATCH 일반 수정 필드는 optional이며 `version`은 required다.
- `title: String?` — optional, DRAFT에서 explicit `null` 허용. 발행 상태에서는 기존 발행 검증을 따른다.
- `content: String?` — optional, DRAFT에서 explicit `null` 허용. 발행 상태에서는 기존 발행 검증을 따른다.
- `representativeFileId: Long?` — optional, explicit `null`이면 대표 이미지 연결 해제
- `imageFileIds: List<Long>` — optional, 전달 시 non-null이며 `[]`이면 첨부 이미지 전체 제거
- `placeIds: List<Long>` — optional·non-null, DRAFT에서만 허용하며 `[]`이면 DRAFT 장소 목록 비우기
- `hashtags: List<String>` — optional·non-null, `[]`이면 해시태그 전체 제거
- `visibility: PUBLIC | MEMO_PRIVATE | PRIVATE` — optional, 전달 시 non-null
- `version: Long` — required

#### 예외사항 및 검증 로직

- 비작성자는 `403 POST_ACCESS_DENIED`다.
- 버전 충돌은 `409 POST_MODIFICATION_CONFLICT`다.
- 발행 Post의 경로 관련 필드 변경은 `409 POST_ROUTE_LOCKED_AFTER_PUBLISH`다.
- 수정 요청에도 제목 50자, 본문 2,000자, 해시태그 5개·각 10자 제한을 동일하게 적용한다.
- 이미 삭제된 요청은 멱등 `204`로 처리한다.

#### 연동 API 엔드포인트

- `PATCH /api/posts/{postId}`
- `DELETE /api/posts/{postId}`

---

## 3.9 커뮤니티

### 3.9.1 기능명: 전체·인기·이웃새 게시글 조회

#### 기능 설명

- `ALL`은 전체 공개 가능한 게시글을 최신순으로 조회한다.
- `POPULAR`은 기간별 인기 점수를 기준으로 조회한다.
- `FOLLOWING`은 로그인 사용자가 팔로우한 사용자의 글을 조회한다.
- 비로그인 사용자는 `ALL`, `POPULAR`을 조회할 수 있다.
- `MEMO_PRIVATE` 게시글을 목록에 포함한다.

#### 세부 로직 및 상태 변화

- 조회 대상은 `status=PUBLISHED`, `visibility IN (PUBLIC, MEMO_PRIVATE)`인 게시글이다.
- `status=BLOCKED`, `PRIVATE`, DRAFT, 삭제 게시글은 제외한다.
- `ALL` 기본 정렬은 `publishedAt DESC`다.
- `FOLLOWING`은 인증을 필수로 한다.
- 사용자 차단 관계가 있으면 서로의 게시글을 결과에서 제외한다.
- 카드의 동행 유형은 단일 `companionType`으로 반환한다.
- 카드 작성자 정보에 `birdType`을 포함한다.

#### 입력 / Request DTO

- `tab: ALL | POPULAR | FOLLOWING`
- `period: WEEK | MONTH | SEASON | YEAR?`
- `cursor: Long?`
- `size: Int = 20` — 최대 50

#### 출력 / Response DTO

- `CommunityPostListResponse`
  - `items: List<CommunityPostCard>` — required, 결과가 없으면 `[]`
    - `postId: Long`
    - `thumbnailUrl: String?`
    - `title: String`
    - `author: AuthorSummary`
      - `userId: Long`
      - `nickname: String?`
      - `birdType: BirdType?`
    - `region: RegionSummary`
    - `companionType: CompanionType`
    - `themes: List<TravelTheme>`
    - `viewCount: Long`
    - `saveCount: Long`
    - `shareCount: Long`
    - `savedRoute: Boolean`
  - `nextCursor: Long?` — required+nullable

#### 예외사항 및 검증 로직

- `FOLLOWING` 비로그인 접근은 `401 UNAUTHORIZED`다.

#### 연동 API 엔드포인트

- `GET /api/community/posts`

---

### 3.9.2 기능명: 인기 기간 필터

#### 기능 설명

- `WEEK`: 최근 7일
- `MONTH`: 최근 30일이며 기본값
- `SEASON`: 최근 3개월
- `YEAR`: 해당 연도 1월 1일부터 현재까지

#### 세부 로직 및 상태 변화

- 기간별 이벤트를 `post_daily_metrics` 또는 원본 히스토리 테이블에서 합산한다.
- 인기 점수는 `조회수 × 1 + 저장수 × 3 + 공유수 × 5`다.
- 동점은 최신 게시글 순으로 정렬한다.
- Redis 캐시는 사용하지 않고 MVP에서는 DB 집계 또는 정기 배치 결과를 사용한다.

#### 연동 API 엔드포인트

- `GET /api/community/posts?tab=POPULAR&period=MONTH`

---

### 3.9.3 기능명: 커뮤니티 검색

#### 기능 설명

- 공개 가능한 커뮤니티 게시글의 지역, 태그, 제목과 본문을 검색한다.
- 초성 검색, 오타 보정과 자동완성은 MVP에서 제외한다.

#### 세부 로직 및 상태 변화

- `status=PUBLISHED`, `visibility IN (PUBLIC, MEMO_PRIVATE)`인 게시글만 검색한다.
- 차단·삭제·비공개·임시 저장 게시글은 제외한다.
- 검색 가중치는 다음 순서다.
  1. 지역명 정확히 일치
  2. 태그 정확히 일치
  3. 제목에 검색어 포함
  4. 본문에 검색어 포함
- 같은 우선순위에서는 `createdAt DESC`로 정렬한다.
- MVP 검색 구현은 MySQL `LIKE` 방식으로 통일하며 Full-Text 인덱스는 사용하지 않는다.
- 지역·태그 정확 일치는 `=` 조건으로 판정하고 제목·본문 포함 검색은 이스케이프 처리한 `LIKE` 조건으로 구현한다.

#### 입력 / Request DTO

- `query: String`
- `sigunguCodes: List<String>?` — 검색·필터에서만 복수 지역 허용
- `theme: TravelTheme?`
- `cursor: Long?`, `size: Int = 20` (최대 50)

#### 출력 / Response DTO

- `items` — required, 결과가 없으면 `[]`
- `nextCursor: Long?` — required+nullable

#### 예외사항 및 검증 로직

- 검색어는 최대 50자이며 초과하면 `400 SEARCH_QUERY_TOO_LONG`을 반환한다.
- `sigunguCodes` 미전달 또는 `null`은 전국 조회, 빈 배열은 빈 결과, 유효·잘못된 코드 혼합은 유효 코드만 적용한다.

#### 연동 API 엔드포인트

- `GET /api/community/posts/search`

---

### 3.9.4 기능명: 조회수 증가

#### 기능 설명

- 로그인 사용자의 게시글 상세 조회를 사용자당 게시글별 24시간에 한 번만 집계한다.

#### 세부 로직 및 상태 변화

- `post_view_histories`에 `postId`, `viewerUserId`, `viewedAt`을 기록한다.
- 동일 사용자의 동일 게시글 조회가 최근 24시간 이내 존재하면 증가시키지 않는다.
- 작성자 본인의 조회는 집계하지 않는다.
- 비로그인 조회는 집계하지 않는다.
- 유효한 최초 조회일 때만 게시글 조회수와 일별 통계를 증가시킨다.
- 사용자 차단 관계 또는 접근 불가 게시글은 집계하지 않는다.

#### 입력 / Request DTO

- Request Body 없음. JWT 사용자 ID를 사용한다.

#### 출력 / Response DTO

- 성공 또는 중복 조회 모두 `204 No Content`

#### 예외사항 및 검증 로직

- 비로그인 호출은 조회수 증가 없이 `204`로 처리하거나 엔드포인트 자체를 호출하지 않도록 한다.
- 접근 불가능한 게시글은 `404 POST_NOT_FOUND`다.

#### 연동 API 엔드포인트

- `POST /api/posts/{postId}/views`

---

### 3.9.5 기능명: 공유수 증가

#### 기능 설명

- 실제 공유 완료 여부와 관계없이 공유 버튼 클릭 수를 집계한다.

#### 세부 로직 및 상태 변화

- 공유 버튼 클릭 시 `shareCount`와 일별 통계를 증가시킨다.
- `PUBLIC`, `MEMO_PRIVATE` 게시글만 공유할 수 있다.
- 채널을 선택적으로 기록한다.

#### 입력 / Request DTO

- `channel: KAKAO | LINK | OTHER`

#### 예외사항 및 검증 로직

- 비공개·차단 게시글은 공유할 수 없다.
- 잘못된 채널은 `400 INVALID_SHARE_CHANNEL`이다.

#### 연동 API 엔드포인트

- `POST /api/posts/{postId}/shares`

---

### 3.9.6 기능명: 게시글 신고

#### 기능 설명

- 로그인 사용자는 운영정책 위반 게시글을 신고할 수 있다.
- 신고 내역은 `reports` 테이블에 저장한다.
- 관리자 화면과 관리자 API는 제공하지 않는다.

#### 세부 로직 및 상태 변화

- JWT 사용자 ID를 신고자로 저장한다.
- 신고 대상 Post 존재 여부와 접근 가능 여부를 검증한다.
- 신고 접수만으로 게시글을 자동 차단하지 않는다.
- 운영자가 DB에서 `posts.status=BLOCKED`로 변경하면 일반 조회에서 즉시 제외한다.
- 동일 사용자는 동일 게시글을 사유와 관계없이 영구적으로 한 번만 신고할 수 있다.
- `reports` 테이블에 `(reporter_user_id, post_id)` 유니크 제약을 적용한다.
- 영구 1회 제한을 유지하기 위해 신고 레코드는 게시글 차단·삭제 여부와 관계없이 운영 이력으로 보관한다.

#### 입력 / Request DTO

- `postId: Long`
- `reasonCode: SPAM | ABUSE | INAPPROPRIATE | OTHER`
- `description: String?`

#### 출력 / Response DTO

- 성공 시 `201 Created`
- `reportId: Long`
- `status: RECEIVED`
- `createdAt: LocalDateTime`

#### 예외사항 및 검증 로직

- 비로그인 요청은 `401 UNAUTHORIZED`다.
- 존재하지 않는 게시글은 `404 POST_NOT_FOUND`다.
- 중복 신고는 `409 REPORT_ALREADY_SUBMITTED`로 처리한다.

#### 연동 API 엔드포인트

- `POST /api/reports`

---

## 3.10 경로 저장

### 3.10.1 기능명: 경로 저장·취소

#### 기능 설명

- 로그인 사용자는 다른 사용자의 `PUBLIC`, `MEMO_PRIVATE` 여행 게시글 경로를 저장할 수 있다.
- 로그인 사용자는 자신이 생성한 AI 결과 미리보기에서 `경로 저장` 버튼을 눌러 해당 경로를 영구 보관할 수 있다.
- 자신의 여행 게시글은 저장할 수 없지만, 자신에게 발급된 AI 미리보기는 저장할 수 있다.
- 저장한 경로는 실제 여행 일정인 내 여행과 구분하여 관리한다.
- 저장한 경로는 경로 데이터를 복사한 별도 스냅샷이 아니라 원본 참조 데이터와의 연결 관계만 유지한다.
- AI 미리보기를 경로로 저장한 이후에도 소유자는 제목, 설명, 해시태그와 Day별 경로 구성을 계속 수정할 수 있다.
- Post 기반 SavedRoute는 `SavedRoute → Post → Trip` 원본 참조 관계를 유지하되, Post 발행 후 Trip 경로 잠금으로 저장한 경로가 예고 없이 바뀌지 않도록 한다.

#### 세부 로직 및 상태 변화

- `SavedRoute`는 `sourceType`과 `sourceId`로 원본을 참조한다.
  - 게시글 경로: `sourceType=POST`, `sourceId=postId`
  - AI 미리보기 경로: `sourceType=AI_PREVIEW`, `sourceId=previewId`
- 저장 관계에는 사용자 ID, 원본 유형, 원본 ID, 저장일과 `sourceAvailable`을 저장한다.
- 게시글 경로 저장 시 Post의 제목·장소·순서·이미지 등의 데이터를 `SavedRoute`에 복사하지 않는다.
- Post의 제목, 본문, 대표 이미지, 첨부 이미지, 해시태그와 공개 범위 등 허용된 비경로 수정은 저장 경로 조회에도 즉시 반영된다.
- Post가 존재하는 동안 연결 Trip의 지역·기간·Day·장소·방문 순서는 잠겨 있으므로 Post 기반 SavedRoute의 경로 구조는 변경되지 않는다.
- 원본 Post가 삭제·비공개·차단되면 저장 관계 행은 즉시 삭제하지 않고 `sourceAvailable=false`로 변경한다.
- `sourceAvailable=false`인 저장 관계는 일반 사용자의 저장 경로 목록에서 노출하지 않으며, 원본 제목·경로·이미지 등의 실제 데이터도 반환하지 않는다.
- AI 미리보기는 생성 시 기본적으로 `retentionStatus=TEMPORARY`, `expiresAt=생성시각+24시간`으로 저장한다.
- 사용자가 AI 결과 화면에서 `경로 저장`을 누르면 다음 작업을 하나의 트랜잭션으로 처리한다.
  1. `previewId`가 현재 사용자의 미리보기인지 확인한다.
  2. 미리보기가 아직 만료되지 않았는지 확인한다.
  3. 해당 미리보기의 `retentionStatus`를 `PERMANENT`로 변경한다.
  4. `expiresAt=null`, `savedAt=현재시각`으로 변경한다.
  5. `sourceType=AI_PREVIEW`, `sourceId=previewId`인 `SavedRoute` 관계를 생성한다.
- 경로 저장된 AI 미리보기는 24시간 만료 및 자동 삭제 대상에서 제외하고 사용자의 저장 공간에 영구 보관한다.
- AI 경로를 저장할 때 별도의 미리보기 복사본이나 스냅샷을 만들지 않고 동일한 `previewId`를 영구 원본으로 전환한다.
- 경로 저장은 AI 미리보기를 읽기 전용으로 잠그는 동작이 아니다.
- 저장 후 사용자가 AI 미리보기를 수정하면 동일한 Preview 원본을 갱신하며 `SavedRoute`에 즉시 반영된다.
- 저장 후 수정 시 최초 `savedAt`은 유지하고 `updatedAt`과 `version`을 갱신한다.
- `jobId`는 AI 작업 상태를 식별하는 값이므로 경로 저장 원본으로 사용하지 않는다.
- 사용자가 `내 여행에 담기`를 누르면 미리보기 내용을 기반으로 `Trip`, `TripDay`, `TripPlace`를 생성한다.
- 이미 생성된 Trip은 Preview와 별도 객체이므로 이후 저장된 AI 경로를 수정해도 기존 Trip에는 자동 반영하지 않는다.
- `내 여행에 담기`와 `경로 저장`은 서로 독립된 기능이다.
- 동일한 원본 경로의 중복 저장 요청은 멱등하게 처리한다.
- 게시글 경로 저장·취소 시 `saveCount` 갱신을 하나의 트랜잭션으로 처리한다.
- AI 미리보기 경로는 커뮤니티 게시글이 아니므로 Post의 `saveCount`에는 포함하지 않는다.
- AI 미리보기 경로 저장 취소 후 해당 `previewId`를 참조하는 활성 SavedRoute 수가 0개가 되면 Preview를 `PERMANENT`에서 `TEMPORARY`로 강등한다.
- 강등 시 `savedAt=null`, `expiresAt=저장 취소 시각+24시간`으로 재설정하며, 이후 24시간 만료 배치가 일반 TEMPORARY Preview와 동일하게 콘텐츠를 삭제한다.
- Preview를 기반으로 이미 생성된 Trip은 별도 객체이므로 Preview 강등·만료의 영향을 받지 않는다.
- 만료 전에 사용자가 다시 경로 저장하면 동일 Preview를 다시 `PERMANENT`로 전환한다.

#### 입력 / Request DTO

- 게시글 경로 저장·취소: Path `postId: Long`
- AI 미리보기 경로 저장·취소: Path `previewId: Long`
- 별도의 Request Body는 없다.
- AI 미리보기 내용 수정 요청은 `3.19.6`에서 처리한다.

#### 출력 / Response DTO

- 저장·취소 성공 시 `204 No Content`를 반환한다.

#### 예외사항 및 검증 로직

- 비로그인 요청은 `401 UNAUTHORIZED`다.
- 존재하지 않는 Post는 `404 POST_NOT_FOUND`다.
- 비공개·차단·삭제 Post는 저장할 수 없다.
- 자신의 Post를 저장하면 `400 CANNOT_SAVE_OWN_ROUTE`를 반환한다.
- 존재하지 않는 AI 미리보기는 `404 AI_PREVIEW_NOT_FOUND`다.
- 다른 사용자의 AI 미리보기이면 `403 AI_PREVIEW_ACCESS_DENIED`다.
- 이미 만료된 미리보기이면 `410 AI_PREVIEW_EXPIRED`다.
- 중복 저장·취소는 멱등하게 처리한다.

#### 연동 API 엔드포인트

- `PUT /api/users/me/saved-routes/posts/{postId}`
- `DELETE /api/users/me/saved-routes/posts/{postId}`
- `PUT /api/users/me/saved-routes/ai-previews/{previewId}`
- `DELETE /api/users/me/saved-routes/ai-previews/{previewId}`

---

### 3.10.2 기능명: 저장한 경로 목록 조회

#### 기능 설명

- 사용자가 저장한 게시글 경로와 영구 저장한 AI 미리보기 경로를 저장일 내림차순으로 조회한다.
- 게시글 경로와 AI 미리보기 경로는 동일한 저장 경로 목록에서 `sourceType`으로 구분한다.
- 저장된 AI 경로는 저장 후 수정 가능한 원본이므로 목록과 상세에서 항상 현재 최신 Preview 내용을 보여준다.
- 게시글 경로는 발행 후 잠긴 Trip의 경로를 조회하므로 저장 시점 이후 경로 구조가 예고 없이 바뀌지 않는다.

#### 세부 로직 및 상태 변화

- `SavedRoute`의 원본 참조를 기준으로 최신 데이터를 조회한다.
- `sourceType=POST`이면 현재 Post와 연결된 Trip·장소·이미지 정보를 조회한다.
- `sourceType=AI_PREVIEW`이면 영구 보관 상태의 AI 미리보기와 PreviewDay·PreviewPlace 정보를 조회한다.
- 영구 저장된 AI 미리보기는 `expiresAt`이 없으며 계속 조회·수정할 수 있다.
- AI 미리보기의 제목, 설명, 해시태그, Day별 장소와 순서가 변경되면 저장 경로 조회 응답에도 즉시 반영한다.
- Post 기반 저장 경로에는 Post의 허용된 비경로 수정만 즉시 반영되며, 연결 Trip의 경로 구조는 Post 삭제 전까지 수정할 수 없다.
- 원본이 삭제·비공개·차단되어 `sourceAvailable=false`가 된 관계는 DB에는 유지하지만 일반 사용자 목록 결과에서는 제외한다.
- 원본 게시글이 삭제되면 타 사용자의 북마크 화면에서 해당 항목과 실제 원본 데이터가 보이지 않는다.
- 같은 Trip에서 새 Post가 발행되어도 과거 삭제 Post의 저장 관계를 새 Post에 연결하지 않는다.
- 커서 페이지네이션을 사용한다.

#### 입력 / Request DTO

- Query `cursor: Long?`
- Query `size: Int = 20` — 최대 50

#### 출력 / Response DTO

- `SavedRouteListResponse`
  - `items: List<SavedRouteListItem>` — required, 결과가 없으면 `[]`
    - `savedRouteId: Long`
    - `sourceType: POST | AI_PREVIEW`
    - `sourceId: Long`
    - `sourceAvailable: Boolean`
    - `title: String`
    - `thumbnailUrl: String?`
    - `author: AuthorSummary?`
    - `region: RegionSummary`
    - `placeNames: List<String>`
    - `themes: List<TravelTheme>`
    - `savedAt: LocalDateTime`
    - `updatedAt: LocalDateTime`
    - `editable: Boolean`
  - `nextCursor: Long?` — required+nullable
- 일반 사용자 목록에는 `sourceAvailable=true`인 항목만 반환한다.
- `sourceType=AI_PREVIEW`이고 현재 사용자가 소유자이면 `editable=true`를 반환한다.
- 다른 사용자의 Post를 참조하는 저장 경로는 `editable=false`를 반환한다.

#### 예외사항 및 검증 로직

- 비로그인 요청은 `401 UNAUTHORIZED`다.
- 영구 저장된 AI 원본 데이터가 비정상적으로 존재하지 않으면 해당 저장 관계를 `sourceAvailable=false`로 변경하고 목록에서 제외한다.

#### 연동 API 엔드포인트

- `GET /api/users/me/saved-routes`

---

## 3.11 포토맵

### 3.11.1 기능명: 전국 포토맵과 방문 지역 수 조회

#### 기능 설명

- 사용자가 발행한 게시글의 장소를 기준으로 포토맵을 생성한다.
- 일정에만 등록되고 게시글로 발행하지 않은 장소는 방문으로 인정하지 않는다.

#### 세부 로직 및 상태 변화

- 삭제되지 않은 `PUBLISHED` Post와 `post_places`를 기준으로 집계한다.
- `PRIVATE` 게시글도 작성자 본인의 포토맵에는 포함한다.
- DRAFT, BLOCKED, 삭제 Post는 제외한다.
- 동일 장소가 여러 게시글에 있어도 `COUNT(DISTINCT placeId)`로 한 곳으로 집계한다.
- 방문 지역 단위는 서비스 공통 지역 기준과 동일한 5자리 시군구다.
- 데이터 증가로 성능이 저하되면 사용자·지역별 집계 테이블을 분리한다.

#### 출력 / Response DTO

- `regions[{region: RegionSummary, visitedPlaceCount, recordCount}]`
- `totalVisitedRegionCount`

#### 연동 API 엔드포인트

- `GET /api/users/me/photomap/regions`

---

### 3.11.2 기능명: 지역별 방문 장소 조회

#### 기능 설명

- 5자리 시군구 코드 기준 지역별 방문 장소와 연결 게시글을 조회한다.
- 동일 장소에 여러 게시글이 있으면 가장 최근 게시글로 기본 이동한다.
- 포토맵 리스트 화면에서는 해당 장소의 나머지 게시글도 확인할 수 있다.

#### 세부 로직 및 상태 변화

- 장소별 가장 최근 `publishedAt`의 Post를 `representativePostId`로 반환한다.
- 같은 장소의 다른 Post ID 목록을 최신순으로 반환한다.
- 삭제·DRAFT·BLOCKED 게시글은 제외하고 작성자 본인의 PRIVATE 게시글은 포함한다.

#### 출력 / Response DTO

- `placeId`, `name`, `latitude`, `longitude`
- `visitCount`
- `representativePostId`
- `postIds`

#### 연동 API 엔드포인트

- `GET /api/users/me/photomap/regions/{regionCode}/places` — `regionCode`는 5자리 시군구 코드

---

## 3.12 홈

### 3.12.1 기능명: 홈 데이터 조회

#### 기능 설명

- 홈에는 오늘의 추천 장소 7개, 축제·이벤트, 추천 기록과 날씨 헤드라인을 표시한다.
- 추천 장소는 전체 사용자에게 공통으로 제공한다.

#### 세부 로직 및 상태 변화

- MVP 추천 장소 7개는 운영자가 DB에서 직접 지정한 고정 장소다.
- 추천 기록은 커뮤니티 인기 점수와 랜덤 셔플을 혼합한다.
- 대상 게시글은 `PUBLISHED`, `PUBLIC` 또는 `MEMO_PRIVATE`이며 BLOCKED·삭제 게시글은 제외한다.
- 날씨 API 공급자는 Open-Meteo로 확정한다.
- 날씨 기준 위치는 사용자의 현재 위치이며 프론트엔드가 `latitude`, `longitude`를 전달한다.
- 위치 권한을 거부하거나 좌표가 없으면 서울 좌표 `37.5665`, `126.9780`을 기본값으로 사용한다.
- 날씨 상태는 `CLEAR | CLOUDY | FOGGY | RAINY | SNOWY | THUNDERSTORM`으로 정규화하고 알 수 없는 WMO 코드는 `CLEAR`로 처리한다.
- 낮/밤은 프론트엔드가 `06:00~18:00`을 낮, 그 외를 밤으로 판단한다.
- Open-Meteo 장애 시 Home API 전체를 실패시키지 않고 `weather=null`을 반환하며 HTTP 상태는 `200 OK`를 유지한다. 프론트엔드는 `날씨 정보를 불러올 수 없습니다.` 문구를 표시한다.
- 축제 데이터는 한국관광공사 TourAPI 국문 관광정보 API에서 매일 새벽 1회 수집·갱신한다.
- 한 섹션 실패로 홈 전체가 실패하지 않도록 `unavailableSections`에 실패한 섹션 코드(예: `PLACES`, `WEATHER`)를 포함하는 부분 응답을 허용한다.
- Redis와 별도 날씨 캐시·TTL 시스템은 구현하지 않는다.

#### 입력 / Request DTO

- `latitude: BigDecimal?`
- `longitude: BigDecimal?`

#### 출력 / Response DTO

- `weather: WeatherSummary?` — 장애 시 Key는 유지하고 값은 `null`
  - `weatherType: CLEAR | CLOUDY | FOGGY | RAINY | SNOWY | THUNDERSTORM`
  - `text`
  - `baseLocation`
- `recommendedPlaces`
- `monthlyEvents`
- `recommendedPosts`
- `unavailableSections: List<String>` — 실패 섹션이 없으면 `[]`

#### 연동 API 엔드포인트

- `GET /api/home`

---

## 3.13 이웃새 및 사용자 차단

### 3.13.1 기능명: 팔로우·언팔로우

#### 기능 설명

- 상호 승인 없는 단방향 팔로우 구조를 사용한다.
- 언팔로우 시 관계 데이터를 삭제한다.
- 비공개 계정과 팔로우 승인 요청은 MVP에서 제공하지 않는다.

#### 세부 로직 및 상태 변화

- `(followerUserId, followingUserId)` 관계를 생성한다.
- 자기 자신 팔로우를 차단한다.
- 중복 팔로우·언팔로우는 멱등 처리한다.
- 차단 관계가 존재하면 팔로우할 수 없다.

#### 연동 API 엔드포인트

- `PUT /api/users/{targetUserId}/follow`
- `DELETE /api/users/{targetUserId}/follow`

---

### 3.13.2 기능명: 팔로워·팔로잉 목록 조회

#### 기능 설명

- 마이페이지와 타인 프로필에서 팔로워 수와 팔로잉 수를 구분한다.
- 각각 클릭하면 해당 사용자 목록을 조회한다.

#### 입력 / Request DTO

- `type: FOLLOWERS | FOLLOWINGS`
- `cursor: Long?`, `size: Int = 20` (최대 50)

#### 출력 / Response DTO

- `items` — required, 결과가 없으면 `[]`
  - `userId`, `nickname`, `birdType`, `trait: PersonalityTrait?`, `followedAt`
- `nextCursor: Long?` — required+nullable

#### 연동 API 엔드포인트

- `GET /api/users/{userId}/follows?type=FOLLOWERS`
- `GET /api/users/{userId}/follows?type=FOLLOWINGS`

---

### 3.13.3 기능명: 사용자 차단·해제·목록 조회

#### 기능 설명

- 로그인 사용자는 특정 사용자를 차단하거나 차단 해제할 수 있다.
- 앱 마켓 심사 대응을 위해 MVP에 포함한다.

#### 세부 로직 및 상태 변화

- `(blockerUserId, blockedUserId)` 관계를 저장한다.
- 자기 자신 차단은 허용하지 않는다.
- 차단 시 두 사용자 사이의 팔로우 관계를 제거한다.
- 차단 관계가 어느 방향으로든 존재하면 서로의 게시글, 프로필과 팔로우 목록 노출을 제한한다.
- 차단 사용자 사이의 팔로우·경로 저장 등 신규 상호작용을 차단한다.
- 기존에 저장한 상대방 경로는 `sourceAvailable=false`로 처리하여 원본 내용을 숨긴다.

#### 입력 / Request DTO

- 차단·해제: Path `targetUserId: Long`
- 목록: `cursor: Long?`, `size: Int = 20` (최대 50)

#### 출력 / Response DTO

- 차단·해제 성공: `204 No Content`
- 목록: `BlockedUsersResponse`
  - `items: List<BlockedUserItem>` — required, 결과가 없으면 `[]`
    - `userId: Long`
    - `nickname: String?`
    - `birdType: BirdType?`
  - `nextCursor: Long?` — required+nullable

#### 예외사항 및 검증 로직

- 비로그인 요청은 `401 UNAUTHORIZED`다.
- 자기 자신 차단은 `400 CANNOT_BLOCK_SELF`다.
- 존재하지 않는 사용자는 `404 USER_NOT_FOUND`다.
- 중복 차단·해제는 멱등 처리한다.

#### 연동 API 엔드포인트

- `PUT /api/users/{targetUserId}/block`
- `DELETE /api/users/{targetUserId}/block`
- `GET /api/users/me/blocked-users`

---

## 3.14 마이페이지

### 3.14.1 기능명: 마이페이지 집계 조회

#### 기능 설명

- 프로필, 파트너 새, 기록 수, 방문 지역 수, 팔로잉 수를 조회한다.
- 내 여행, 캘린더, 저장 경로와 저장 장소 메뉴를 제공한다.
- 리워드 포인트 메뉴는 MVP에서 제외한다.

#### 세부 로직 및 상태 변화

- 기록 수는 삭제되지 않은 Post 중 DRAFT를 제외하여 집계한다.
- PRIVATE 게시글은 기록 수에 포함한다.
- BLOCKED 게시글은 작성자 본인의 `postCount`에서도 제외한다.
- 방문 지역 수는 포토맵과 동일하게 5자리 시군구 기준이다.
- 대표 이웃새 수치는 사용자가 팔로우한 `followingCount`다.
- 팔로워 수와 팔로잉 수를 각각 반환한다.
- 파트너 새는 `birdType`만 기준으로 프론트 이미지와 매핑한다.

#### 출력 / Response DTO

- `profile{userId, nickname, introduction, birdType}`
- `statistics.postCount`
- `statistics.visitedRegionCount`
- `statistics.followerCount`
- `statistics.followingCount`

#### 연동 API 엔드포인트

- `GET /api/users/me/mypage`

---

## 3.15 리워드 포인트

- `[MVP 제외]` 리워드 포인트 잔액, 적립, 사용, 거래내역과 관련 API는 MVP에서 완전히 제외한다.
- `reward_accounts`, `reward_transactions` 테이블과 `/api/users/me/rewards`, `/api/users/me/reward-transactions` API를 구현하지 않는다.

---

## 3.16 통합 검색

### 3.16.1 기능명: 장소·기록·지역 통합 검색

#### 기능 설명

- 로그인 사용자는 홈에서 장소, 여행 기록과 지역을 통합 검색한다.
- 결과 노출 우선순위는 장소 → 기록 → 지역이다.
- 각 타입별 최대 5개를 반환한다.
- 사용자 계정 검색은 포함하지 않는다.
- 최근 검색어 저장과 인기 검색어 집계는 MVP에서 제외한다.

#### 세부 로직 및 상태 변화

- 장소 검색과 공개 게시글 검색을 조합한다.
- 게시글은 `PUBLISHED`, `PUBLIC` 또는 `MEMO_PRIVATE`만 포함하고 BLOCKED·삭제 게시글을 제외한다.
- 장소 API 일일 한도가 초과되면 장소 섹션에 오류를 표시하고 기록·지역 결과만 부분 반환할 수 있다.
- 초성 검색, 오타 보정과 자동완성은 제공하지 않는다.

#### 입력 / Request DTO

- `query: String`
- `limitPerType: Int = 5`이며 최대 5
- `sigunguCodes: List<String>?` — 검색·필터에서만 복수 지역 허용

#### 출력 / Response DTO

- 응답 표시 순서: `places`, `posts`, `regions: List<RegionSummary>`
- 각 목록 최대 5개
- `hasMorePlaces`, `hasMorePosts`, `hasMoreRegions`
- 부분 실패 시 `unavailableSections`를 반환할 수 있다.

#### 예외사항 및 검증 로직

- 비로그인 요청은 `401 UNAUTHORIZED`다.
- 빈 검색어는 `400 SEARCH_QUERY_REQUIRED`다.

#### 연동 API 엔드포인트

- `GET /api/search`

---

## 3.17 축제·이벤트

### 3.17.1 기능명: 진행·예정 축제 목록 및 상세 조회

#### 기능 설명

- 홈과 전체보기 화면에 진행 중이거나 예정된 국내 축제를 표시한다.
- 종료된 축제는 MVP 목록과 과거 기록에서 노출하지 않는다.
- 축제 저장·찜 기능은 MVP에서 제외하고 프론트 하트 버튼도 제거한다.

#### 세부 로직 및 상태 변화

- 한국관광공사 TourAPI 국문 관광정보 API에서 데이터를 수집한다.
- 매일 새벽 1회 배치 스케줄러로 내부 DB에 동기화한다.
- `endDate < 오늘`인 축제는 조회에서 제외한다.
- 상태는 `UPCOMING`, `ONGOING`만 반환한다.

#### 입력 / Request DTO

- `from: LocalDate`
- `to: LocalDate`
- `sigunguCodes: List<String>?` — 검색·필터에서만 복수 지역 허용
- `cursor: Long?`, `size: Int = 20` (최대 50)

#### 출력 / Response DTO

- `eventId`, `name`, `region: RegionSummary`, `placeName`
- `startDate`, `endDate`, `thumbnailUrl`
- `status: UPCOMING | ONGOING`
- 목록 응답의 `nextCursor: Long?` — required+nullable

#### 연동 API 엔드포인트

- `GET /api/events`
- `GET /api/events/{eventId}`

---

## 3.18 지도·경로 표시

### 3.18.1 기능명: Day별 이동 경로 조회

#### 기능 설명

- 선택 Day의 장소를 방문 순서 번호 핀과 단순 직선 점선으로 연결한다.
- 이동 수단 선택, 예상 이동시간과 총 거리는 제공하지 않는다.
- 네이버 Directions API는 MVP에서 호출하지 않는다.

#### 세부 로직 및 상태 변화

- Trip의 Day 장소 좌표를 `order` 순으로 반환한다.
- 프론트엔드는 인접 좌표를 직선 점선으로 연결한다.
- 별도 경로 선 데이터를 DB에 저장하지 않는다.
- Trip 공개 범위에 따라 접근 권한을 검증한다.
  - 소유자: 전체 접근
  - `PUBLIC`, `MEMO_PRIVATE`: 로그인·비로그인 접근 허용
  - `PRIVATE`: 소유자만 허용
- `jobId`, `previewId`는 본 API에서 사용하지 않는다.

#### 입력 / Request DTO

- Path `tripId: Long`, `dayNumber: Int`
- 이동 수단 Query Parameter는 받지 않는다.

#### 출력 / Response DTO

- `tripId`, `dayNumber`
- `places[{tripPlaceId, order, placeId, latitude, longitude}]`
- `routePoints[{latitude, longitude}]`
- `totalDistanceMeters`, `estimatedDurationMinutes`는 반환하지 않는다.

#### 예외사항 및 검증 로직

- 존재하지 않는 일정은 `404 TRIP_NOT_FOUND`다.
- PRIVATE 비소유자 접근은 `404 TRIP_NOT_FOUND`다.
- Day가 없으면 `404 TRIP_DAY_NOT_FOUND`다.
- 장소가 0~1개면 좌표만 정상 반환한다.

#### 연동 API 엔드포인트

- `GET /api/trips/{tripId}/days/{dayNumber}/route`

---

## 3.19 AI 여행 추천

### 3.19.1 기능명: 일반 조건 기반 AI 일정 추천 요청

#### 기능 설명

- 사용자는 5자리 시군구 코드 한 개, 날짜, 단일 동행 유형, 테마와 일정 밀도로 AI 추천을 요청한다. 여행 생성용 AI 요청에서는 복수 지역 배열을 사용하지 않는다.
- AI·데이터 파트는 사전 수집·정규화한 관광지 데이터에서 후보 장소를 선정한다.
- 추천 요청 시 관광 API를 실시간 호출하지 않는다.
- 결과는 Trip에 즉시 저장하지 않고 AI 일정 미리보기로 제공한다.
- 생성된 미리보기는 기본적으로 24시간 동안 유효하지만, 사용자가 `경로 저장`을 실행하면 영구 보관 상태로 전환된다.
- `pace`는 `RELAXED | NORMAL | DENSE`를 사용하며 하루 장소 수를 고정하는 숫자 제한으로 해석하지 않는다.

#### 세부 로직 및 상태 변화

- 클라이언트는 Spring Boot만 호출하며 AI 서버를 직접 호출하지 않는다.
- 사용자별 AI 추천은 하루 최대 3회다.
- 일일 기준은 `Asia/Seoul` 00:00~23:59로 집계한다.
- Spring Boot가 AI job을 `QUEUED`로 생성하고 `jobId: Long`을 발급한다.
- 백엔드 내부 비동기 디스패처는 AI 추천에 참조되는 런타임 신규 장소를 먼저 AI 추천 데이터에 동기화한 뒤 `POST /internal/v1/recommendations`로 작업을 전달한다.
- 동기화 대상은 `savedPlaceIds`, `wishlistPlaceIds`, `existingSchedule[].placeIds` 중 초기 TourAPI 매핑(`KTO_TOUR_API`)이 없어 AI의 사전 데이터에 존재한다고 보장할 수 없는 canonical `placeId`다. `GENERAL`처럼 참조 장소 ID가 없으면 이 단계는 생략한다.
- Backend → AI 장소 동기화는 `PUT /internal/v1/places/sync`를 사용하며 `X-Internal-AI-Key` 인증을 적용한다. Request는 `places[]`에 `placeId`, `regionCode`, `name`, `address`, `latitude`, `longitude`, `category`를 전달하고 AI는 `placeId` 기준으로 멱등 upsert한다.
- 장소 동기화가 `204 No Content`로 성공한 뒤에만 `POST /internal/v1/recommendations`를 호출한다. 동기화가 실패하면 추천 요청을 보내지 않고 Backend job을 `FAILED`로 변경하여 `AI_PLACE_SYNC_FAILED`를 기록한다. 공개 Recommendation Request/Response DTO는 변경하지 않는다.
- Spring Boot → AI 요청에도 Callback과 동일한 `X-Internal-AI-Key: {TRAVELBIRD_AI_CALLBACK_KEY}` Header를 필수로 전달한다. Header 누락 또는 Key 불일치는 AI 서버가 `401 INVALID_INTERNAL_AI_KEY`로 거부한다.
- AI 서버는 신규 요청 접수 성공 시 즉시 `202 Accepted`와 `status=PENDING`, 동일한 `jobId`, `acceptedAt`을 반환하며 Spring Boot는 이를 확인한 뒤 Backend job을 `PROCESSING`으로 변경한다.
- 동일 `jobId`와 동일 Payload가 재전송되면 AI 서버는 기존 작업을 재사용하고 기존 접수 결과를 `202 Accepted`로 멱등 반환한다. 동일 `jobId`에 다른 Payload가 전달되면 `409 AI_JOB_ID_CONFLICT`를 반환한다.
- 동일 `jobId`의 동일 Payload 여부는 `jobId`를 제외한 `RecommendationJobRequest`의 정규화된 의미 Payload를 SHA-256 fingerprint로 비교하여 판단한다. JSON 객체 Key 순서, 공백, 개행은 무시한다. 순서가 의미 없는 `themes`, `savedPlaceIds`, `wishlistPlaceIds`는 정렬하여 비교하고, `existingSchedule`은 `dayNumber` 기준으로 정규화하되 각 Day의 `placeIds` 순서는 유지한다. 동일 fingerprint면 기존 접수 결과를 `202 Accepted`로 멱등 반환하고, 다르면 `409 AI_JOB_ID_CONFLICT`를 반환한다.
- AI 서버가 요청을 접수하기 전에 Request 자체를 거부하는 즉시 응답은 `400 INVALID_REQUEST`, `401 INVALID_INTERNAL_AI_KEY`, `409 AI_JOB_ID_CONFLICT`, `422 PLACE_NOT_FOUND | PLACE_REGION_MISMATCH | CONFLICTING_PLACE_POLICY`를 사용한다.
- Callback timeout 카운트는 Backend Job이 `PROCESSING`으로 전환된 시점부터 시작하며 제한시간은 **180초**다.
- AI 서버의 `RUNNING`은 AI 내부 실행 상태이며 Backend는 별도 중간 Callback 없이 `PROCESSING`을 유지한다.
- AI 서버는 생성 완료 또는 실패 후 `POST /internal/ai-callbacks/trip-recommendations`로 `COMPLETED` 또는 `FAILED` 결과를 전달한다.
- `COMPLETED` Callback 결과 검증과 preview 저장이 모두 완료되면 Backend job을 `SUCCEEDED`로 변경한다.
- `FAILED` Callback을 정상 수신하면 Backend job을 `FAILED`로 변경하고 Preview를 생성하지 않는다.
- 미리보기 생성 시 `retentionStatus=TEMPORARY`, `expiresAt=생성시각+24시간`을 설정한다.
- 사용자가 아무 저장 동작도 하지 않은 미리보기는 24시간 후 `EXPIRED`로 변경하고 접근을 차단한다.
- 사용자가 `경로 저장`을 누르면 동일한 미리보기를 `PERMANENT`로 전환하고 `expiresAt`을 제거하여 영구 보관한다.
- 사용자가 `내 여행에 담기`를 누르면 미리보기 내용을 기반으로 실제 Trip을 생성한다.
- 클라이언트는 Spring Boot의 job 상태 API를 폴링한다.
- 진행 중 작업 취소는 지원하지 않는다.

#### 입력 / Request DTO

- `regionCode: String` — 5자리 시군구 코드 한 개
- `startDate: LocalDate`
- `endDate: LocalDate`
- `companionType: CompanionType`
- `themes: List<TravelTheme>` — 1~3개, 중복 불가
- `pace: RELAXED | NORMAL | DENSE`
- `requestType: GENERAL`

#### 출력 / Response DTO

- `202 Accepted`
- `jobId: Long`
- `status: QUEUED`
- `requestedAt`
- `statusUrl`

#### Backend → AI 공통 내부 Request DTO

- 내부 DTO명은 `RecommendationJobRequest`를 사용한다.
- `jobId: Long` — Spring Boot가 발급한 작업 ID
- `requestType: GENERAL | SAVED_PLACES | TRIP_WISHLIST`
- `regionCode: String` — 5자리 시군구 코드 한 개
- `startDate: LocalDate`
- `endDate: LocalDate`
- `companionType: CompanionType`
- `themes: List<TravelTheme>` — `ACTIVITY | SNS_HOTPLACE | NATURE | ATTRACTION | SHOPPING | FOOD`, 1~3개
- `pace: RELAXED | NORMAL | DENSE`
- `savedPlaceIds: List<Long>` — 내부 공통 Request에서는 non-null 배열로 전달하며 해당하지 않으면 `[]`. 공개 `SAVED_PLACES` 요청에서는 최소 1개가 필요하고 `null`은 허용하지 않는다.
- `wishlistPlaceIds: List<Long>` — 해당하지 않으면 `[]`
- `existingSchedule: List<ExistingScheduleDay>` — 해당하지 않으면 `[]`
  - `dayNumber: Int`
  - `placeIds: List<Long>`
- `allowAdditionalRecommendations: Boolean`
- `requestType=GENERAL`에서는 신규 장소 추천이 기능의 본체이므로 Backend가 `allowAdditionalRecommendations=true`로 전달한다.
- 내부 요청은 `travelDays`를 원본 입력값으로 사용하지 않는다. 여행 일수 계산이 필요하면 AI 서버가 `startDate`, `endDate`로 계산한다.
- `jobId`와 모든 장소 ID는 `Long/int64` 의미를 유지하며 JSON에서는 `number`로 전달하고 Java `Integer`로 강제 Casting하지 않는다.

#### AI 서버 즉시 응답 규격

- `202 Accepted`
  - 신규 정상 요청을 접수한 경우
  - 동일 `jobId` + 동일 Payload가 재전송된 경우 새 작업을 만들지 않고 기존 접수 결과를 멱등 반환
  - Response DTO는 `AiServerAcceptedResponse`
    - `jobId: Long`
    - `status: PENDING`
    - `acceptedAt: LocalDateTime`
  - AI 서버는 Spring Boot가 전달한 `jobId`와 동일한 값을 반환한다.
- `400 INVALID_REQUEST`
  - 필수 필드 누락
  - 타입·형식 오류
  - 허용되지 않은 Enum
  - `RecommendationJobRequest` Schema 제약 위반
- `401 INVALID_INTERNAL_AI_KEY`
  - `X-Internal-AI-Key` 누락 또는 불일치
- `409 AI_JOB_ID_CONFLICT`
  - 동일 `jobId`가 이미 존재하지만 기존 요청과 다른 Payload가 전달된 경우
- `422 PLACE_NOT_FOUND`
  - Backend의 추천 전 장소 동기화 이후에도 전달된 내부 `placeId`를 AI 추천 데이터에서 확인할 수 없는 방어적 오류
- `422 PLACE_REGION_MISMATCH`
  - 전달된 장소가 요청 `regionCode`와 일치하지 않는 경우
- `422 CONFLICTING_PLACE_POLICY`
  - `requestType`, 기존 일정, 저장 장소, 위시리스트 또는 추가 추천 정책 사이의 의미적 충돌이 있는 경우
- `400/409/422` 즉시 실패 Response는 모두 공통 `ErrorResponse{code, message, timestamp, details}`를 사용하며 AI 전용 중첩 `ErrorBody`를 만들지 않는다.

#### 예외사항 및 검증 로직

- 비로그인 요청은 `401 UNAUTHORIZED`다.
- 일일 3회 초과는 `429 AI_DAILY_REQUEST_LIMIT_EXCEEDED`다.
- 잘못된 날짜·지역·동행·테마·밀도는 각각 `400` 오류로 처리한다.
- AI 서버로 작업을 전달하거나 `202 PENDING` 접수 응답을 확인하기 전에 사용자 요청 자체를 완료할 수 없는 경우 `503 AI_SERVICE_UNAVAILABLE`로 처리한다.
- 사용자에게 Backend `202`와 `jobId`를 이미 반환한 뒤 비동기 전달 단계에서 AI 접수가 실패한 경우에는 기존 HTTP 응답을 바꿀 수 없으므로 해당 Backend job을 `FAILED`로 변경하고 상태 조회에서 오류를 제공한다.
- Backend Job이 `PROCESSING`으로 전환된 시점부터 180초 안에 Callback이 도착하지 않으면 job을 `FAILED`로 변경하고 `AI_CALLBACK_TIMEOUT`을 기록한다.
- `AI_CALLBACK_TIMEOUT`으로 Backend job이 이미 `FAILED`가 된 뒤 늦은 `COMPLETED` Callback이 도착하면 Job을 `SUCCEEDED`로 되살리거나 Preview를 생성하지 않는다. 기존 `FAILED` 상태를 유지하고 Callback 요청 자체는 멱등하게 `200 OK`로 처리한다.
- AI 내부 동기 요청 오류가 발생하더라도 해당 내부 오류 Body를 프론트엔드에 그대로 전달하지 않고 Spring Boot 공통 `ErrorResponse`로 매핑한다.
- `POST /internal/v1/recommendations`의 `400/422`는 AI 서버의 방어적 검증이다. 사용자 요청의 소유권, 선택한 저장 장소가 실제 사용자 저장 장소인지 여부, Trip 위시리스트와 기존 일정 상태, `requiredPlaceCount > dayCount × 15` 수용량 검증 등 Spring Boot가 알고 있는 정책은 AI 서버 호출 전에 우선 검증한다.

#### 연동 API 엔드포인트

- 사용자 API: `POST /api/ai/trip-recommendations`
- Backend → AI 장소 동기화 내부 API: `PUT /internal/v1/places/sync`
- Backend → AI 추천 내부 API: `POST /internal/v1/recommendations`

---

### 3.19.2 기능명: 저장 장소 기반 AI 일정 추천 요청

#### 기능 설명

- 사용자는 자신이 저장한 장소를 선택하여 AI 추천 우선순위에 반영한다.
- `SAVED_PLACES`의 `savedPlaceIds`는 사용자의 선호를 나타내는 참고 신호이며 일정 필수 포함(`MUST_INCLUDE`) 대상이 아니다.
- AI는 선택하지 않은 새로운 장소를 추가 추천할 수 있다.
- 저장 장소 기반 추천과 특정 Trip의 위시리스트 기반 추천은 정책을 분리한다. `SAVED_PLACES`는 추천 참고, `TRIP_WISHLIST`는 필수 포함으로 해석한다.

#### 세부 로직 및 상태 변화

- 선택한 `placeId`가 요청 사용자의 저장 장소인지 검증한다.
- 선택 가능한 장소는 요청의 단일 `regionCode`(5자리 시군구 코드)와 동일 지역으로 제한한다.
- 최소 선택 장소 수는 1개다.
- 선택 장소 좌표·카테고리·지역정보를 AI에 전달한다.
- AI·데이터 파트가 정규화 데이터에서 후보 장소를 선정한다.
- Backend → AI 내부 Request에는 `requestType=SAVED_PLACES`, 선택된 `savedPlaceIds`를 전달하며 `existingSchedule`과 `wishlistPlaceIds`는 이 흐름에서 사용하지 않으면 빈 배열 `[]`로 전달한다.
- 저장 장소 기반 추천은 새로운 장소 추천을 허용하는 기존 기능이므로 Backend가 내부 AI Request의 `allowAdditionalRecommendations=true`를 사용한다.
- 결과는 preview로 저장한다.

#### 입력 / Request DTO

- 일반 조건
- `requestType: SAVED_PLACES`
- `savedPlaceIds: List<Long>` — 조건부 필수, 최소 1개, non-null
- `requestType=GENERAL`에서는 `savedPlaceIds`를 미전달할 수 있다. `requestType=SAVED_PLACES`에서는 필드가 반드시 존재해야 하며 `null`은 허용하지 않는다.

#### 예외사항 및 검증 로직

- 저장하지 않은 장소는 `403 SAVED_PLACE_ACCESS_DENIED`다.
- 선택 장소가 없으면 `422 INSUFFICIENT_SAVED_PLACES`다.
- 다른 지역 장소는 `422 INCOMPATIBLE_PLACE_REGIONS`다.

#### 연동 API 엔드포인트

- `POST /api/ai/trip-recommendations`

---

### 3.19.3 기능명: 일정 위시리스트 기반 AI 경로 배치 요청

#### 기능 설명

- 특정 Trip의 현재 일정과 위시리스트 장소를 함께 고려하여 AI가 최종 Day별 경로를 다시 구성한다.
- 위시리스트 장소는 모두 일정에 반드시 포함한다.
- 현재 Trip에 이미 배치된 장소도 모두 유지하며 AI 추천 때문에 삭제하지 않는다.
- 기존 장소의 현재 Day와 방문 순서는 고정값이 아니라 현재 상태를 나타내는 참고 정보이며, AI는 위시리스트와 필요 시 추가 추천 장소를 함께 고려해 전체 Day와 방문 순서를 재구성할 수 있다.
- 결과는 기존 Trip에 즉시 반영하지 않고 preview로 제공한다.
- 발행된 Post가 연결되어 경로가 잠긴 Trip에는 AI 경로 배치 요청 자체를 허용하지 않는다.

#### 세부 로직 및 상태 변화

- Trip 소유권과 수정 가능 여부를 검증한다.
- `cancelledAt`이 존재하는 Trip은 AI 경로 배치 요청을 `409 TRIP_CANCELLED_READ_ONLY`로 차단한다.
- AI 비용 발생 전에 연결된 발행 Post 존재 여부를 먼저 검증한다.
- 위시리스트가 비어 있으면 요청을 차단한다.
- 프론트엔드는 `existingSchedule`을 직접 전달하지 않는다. Spring Boot가 `tripId`로 현재 `TripDay`, `TripPlace`를 조회하여 Backend → AI 내부 Request의 `existingSchedule`을 생성한다.
- `existingSchedule`은 `[{dayNumber, placeIds}]` 구조이며 `placeIds`는 서비스 내부 `Long placeId`를 JSON number로 전달한다.
- 기존 장소는 MVP에서 항상 KEEP한다. 기존 `existingPlaceMode: KEEP | REPLACE` 선택 기능은 제거하며 기존 장소를 추천 결과에서 탈락시키는 REPLACE 정책을 사용하지 않는다.
- `wishlistPlaceIds`는 전부 `MUST_INCLUDE`이며 최종 결과에서 하나라도 누락되어서는 안 된다.
- AI 호출 전에 Spring Boot는 필수 배치 장소 수를 `count(distinct(existingSchedule의 placeId ∪ wishlistPlaceIds))`로 계산한다. 이 값이 `여행 Day 수 × 15`를 초과하면 AI 서버를 호출하지 않고 `422 AI_ROUTE_REQUIRED_PLACE_LIMIT_EXCEEDED`를 반환한다. 이 검증은 AI가 해결할 수 없는 물리적 수용량 초과를 사전에 차단하기 위한 하드 검증이다.
- `allowAdditionalRecommendations=true`이면 기존 장소와 필수 위시리스트 외에 보완 장소를 추가 추천할 수 있다.
- `allowAdditionalRecommendations=false`이면 새로운 보완 장소만 추가하지 않는다. 이 값이 `false`여도 `existingSchedule`의 기존 장소와 `TRIP_WISHLIST`의 필수 위시리스트 장소를 제거하지 않는다.
- AI는 신규 추천 장소만 반환하지 않고 기존 장소, 필수 위시리스트, 허용된 추가 추천 장소를 모두 포함한 최종 전체 `days → places → placeId/order`를 Callback으로 반환한다.
- Spring Boot는 기존 일정과 AI 신규 장소를 자체 merge하지 않고 AI가 반환한 전체 결과를 검증한 뒤 Preview로 저장한다.
- preview 생성 시 기존 Trip을 변경하지 않고 사용자가 확정할 때만 반영한다.
- 미리보기 적용 시점에도 경로 잠금을 다시 검증하여, 요청 이후 Post가 발행된 경쟁 상황을 차단한다.

#### 입력 / Request DTO

- Path `tripId: Long`
- `requestType: TRIP_WISHLIST`
- `allowAdditionalRecommendations: Boolean = true`
- `existingSchedule`은 Request Body에서 받지 않고 Spring Boot가 DB에서 생성한다.

#### Backend → AI 추가 내부 데이터

- `requestType: TRIP_WISHLIST`
- `wishlistPlaceIds: List<Long>` — 모두 필수 포함
- `existingSchedule: List<ExistingScheduleDay>`
  - `dayNumber: Int`
  - `placeIds: List<Long>`
- `allowAdditionalRecommendations: Boolean`
- 기존 장소의 현재 `dayNumber`와 `placeIds` 배열 순서는 AI가 현재 배치를 이해하기 위한 입력 정보이며 최종 배치를 고정하지 않는다.

#### 예외사항 및 검증 로직

- 빈 위시리스트는 `400 EMPTY_WISHLIST`다.
- `count(distinct(existingSchedule placeIds ∪ wishlistPlaceIds)) > 여행 Day 수 × 15`이면 AI 호출 전에 `422 AI_ROUTE_REQUIRED_PLACE_LIMIT_EXCEEDED`를 반환한다.
- 비소유자는 `403 TRIP_ACCESS_DENIED`다.
- 존재하지 않는 장소는 `404 PLACE_NOT_FOUND`다.
- 발행 Post가 연결된 Trip은 `409 TRIP_ROUTE_LOCKED_BY_PUBLISHED_POST`다.
- 취소된 Trip은 `409 TRIP_CANCELLED_READ_ONLY`다.

#### 연동 API 엔드포인트

- `POST /api/trips/{tripId}/ai-route-recommendations`

---

### 3.19.4 기능명: AI 작업 상태 조회

#### 기능 설명

- 클라이언트는 `jobId`로 상태를 폴링한다.
- 실시간 SSE, WebSocket과 Push 완료 알림은 MVP에서 제외한다.
- 진행 중 작업 취소는 지원하지 않는다.
- `jobId`는 작업 상태 조회에만 사용하며 경로 저장의 원본 식별자로 사용하지 않는다.
- `jobId`는 Spring Boot가 생성한 `Long` 값이며 AI 서버와 Backend가 동일한 값을 공유한다.

#### 세부 로직 및 상태 변화

- Backend 상태는 `QUEUED`, `PROCESSING`, `SUCCEEDED`, `FAILED`, `EXPIRED`다.
- `QUEUED`: 처리 대기
- `PROCESSING`: AI 서버 접수 이후 후보 선정과 일정 생성 진행
- `SUCCEEDED`: AI `COMPLETED` 결과를 Spring Boot가 검증하고 preview 저장까지 완료
- `FAILED`: AI 처리·Callback·검증·저장 실패
- `EXPIRED`: 미저장 임시 미리보기의 유효기간이 끝나 상세 내용이 삭제된 상태
- AI 서버 내부 Job 상태는 `PENDING | RUNNING | COMPLETED | FAILED`의 4개를 유지하고, Backend Job 상태는 `QUEUED | PROCESSING | SUCCEEDED | FAILED | EXPIRED`의 5개를 유지한다. 두 Enum을 억지로 하나로 합치지 않는다.
- 상태 매핑은 다음을 기준으로 한다.
  - Spring Boot Job 생성 → Backend `QUEUED`
  - AI 서버 `202 + PENDING` 접수 확인 → Backend `PROCESSING`
  - AI 내부 `RUNNING` → Backend `PROCESSING` 유지
  - AI `COMPLETED` Callback → Spring Boot 결과 검증 및 Preview 저장 성공 후 Backend `SUCCEEDED`
  - AI `FAILED` Callback → Backend `FAILED`
  - TEMPORARY Preview 만료 → Backend `EXPIRED`
- AI의 `COMPLETED`는 Backend의 `SUCCEEDED`와 동일한 의미가 아니다. Spring Boot의 placeId·Day·order·필수 포함 정책 검증과 Preview 저장까지 성공해야 `SUCCEEDED`다.
- 성공 상태에서만 사용 가능한 `previewId`를 반환한다.
- `retentionStatus=TEMPORARY`인 미저장 미리보기만 24시간 만료 대상으로 한다.
- 사용자가 `경로 저장`을 실행하여 `retentionStatus=PERMANENT`가 된 미리보기는 만료하지 않는다.
- 영구 저장된 미리보기와 연결된 AI 작업은 `SUCCEEDED` 상태를 유지한다.
- `TEMPORARY` 미리보기는 만료 전까지 수정할 수 있고 `PERMANENT` 미리보기는 저장 후에도 계속 수정할 수 있다.
- 미리보기 수정 자체는 현재 `expiresAt`을 연장하지 않는다.
- 마지막 AI SavedRoute 취소로 `PERMANENT → TEMPORARY` 강등된 경우에만 `expiresAt`을 취소 시각부터 24시간 후로 새로 설정한다.
- 강등 직후 연결 AI Job은 `SUCCEEDED`를 유지하고, 새 `expiresAt`이 지난 뒤 만료 배치가 실행되면 `EXPIRED`로 변경한다.
- 사용자가 `내 여행에 담기`만 실행하고 경로 저장하지 않은 경우 Trip은 영구 유지되지만 임시 Preview는 24시간 후 만료된다.
- 만료 스케줄러는 `expiresAt <= now`, `retentionStatus=TEMPORARY`, `SavedRoute 없음` 조건을 모두 충족한 Preview만 처리한다.
- 만료 처리 시 PreviewDay, PreviewPlace, 해시태그, 제목과 설명 등 미리보기 내용을 삭제한다.
- `previewId`, 소유자 ID, 생성·만료시각과 상태만 가진 최소 tombstone 행은 유지하여 기존 preview 접근에 `410 AI_PREVIEW_EXPIRED`를 반환할 수 있게 한다.
- 연결 AI Job 행은 삭제하지 않고 `status=EXPIRED`, `expiredAt`을 기록하여 MVP 기간 동안 계속 보관한다.
- AI Job에는 요청 시각, 요청 유형, 처리시간, 성공·실패·만료 상태와 오류코드 등 운영 분석에 필요한 최소 메타데이터를 유지한다.
- MVP에서는 EXPIRED AI Job의 자동 삭제 기간을 두지 않는다.

#### 출력 / Response DTO

- `jobId: Long`
- `status: QUEUED | PROCESSING | SUCCEEDED | FAILED | EXPIRED`
- `previewId: Long?` — Key는 required, Preview가 없으면 `null`
- `previewAvailable: Boolean`
- `previewRetentionStatus: TEMPORARY | PERMANENT | EXPIRED | null` — Key는 required+nullable
  - `QUEUED | PROCESSING | FAILED`처럼 Preview가 존재하지 않으면 `null`
  - `SUCCEEDED`이면 `TEMPORARY | PERMANENT`
  - `EXPIRED`이면 `EXPIRED`
- `editable: Boolean`
- `requestedAt: LocalDateTime` — required·non-null
- `startedAt: LocalDateTime?`
- `completedAt: LocalDateTime?`
- `expiresAt: LocalDateTime?`
- `expiredAt: LocalDateTime?`
- `savedAt: LocalDateTime?`
- `error{code, message, retryable}`
- `EXPIRED` 응답에서는 추적을 위해 `previewId`를 유지할 수 있으나 `previewAvailable=false`, `editable=false`를 반환한다.
- `error` 필드는 Job 상태 조회의 진단 정보이며 API 요청 자체의 실패 응답은 공통 `ErrorResponse`를 사용한다.

#### 예외사항 및 검증 로직

- 없는 작업은 `404 AI_JOB_NOT_FOUND`다.
- 타인 작업은 `403 AI_JOB_ACCESS_DENIED`다.
- 만료 작업은 상태 응답에서 `EXPIRED`를 반환한다.
- 만료 Preview 상세 접근은 `410 AI_PREVIEW_EXPIRED`로 처리한다.
- 영구 저장된 미리보기에는 `expiresAt=null`, `previewAvailable=true`, `editable=true`를 반환한다.

#### 연동 API 엔드포인트

- `GET /api/ai/trip-recommendations/{jobId}`

---

### 3.19.5 기능명: AI 결과 검증 및 일정 미리보기 생성

#### 기능 설명

- AI는 일정 제목, 설명과 기존 일정 장소·필수 위시리스트·허용된 추가 추천 장소를 모두 포함한 최종 전체 Day별 일정과 방문 순서를 반환한다.
- MVP에서는 방문 추천 시간과 체류시간을 사용하지 않는다.
- `placeId`, `order`, `reason`은 필수다.
- AI 결과에 일부 오류가 있으면 전체 결과를 실패 처리한다.
- 검증된 결과는 실제 Trip이 아니라 24시간 유효한 임시 미리보기로 최초 생성한다.
- 생성된 Preview는 임시 또는 영구 보관 상태와 관계없이 소유자가 수정할 수 있는 경로 원본이다.

#### 세부 로직 및 상태 변화

- AI·데이터 파트가 정규화 관광지 데이터에서 후보를 선정한다.
- 백엔드는 반환된 모든 `placeId`가 내부 DB에 존재하는지 검증한다.
- 후보 장소 선정 주체가 AI·데이터 파트이므로 백엔드가 별도 전달 후보목록 포함 여부를 일반 추천에서 필수 검증하지 않는다.
- Day 번호가 여행 기간 안에 있는지 검증한다.
- Day별 `order` 누락·중복을 검증한다.
- AI가 생성한 Preview도 직접 일정·Preview 수정 정책과 동일하게 Day별 장소 수 최대 15개를 초과할 수 없으며 초과 시 전체 결과를 실패 처리한다.
- AI 결과의 전체 `days`를 기준으로 동일한 `placeId`가 둘 이상 존재하는지 검증한다. 같은 Day뿐 아니라 서로 다른 Day에 같은 장소가 반복되어도 허용하지 않는다.
- 전체 일정에서 동일 `placeId`가 중복되면 Callback 결과를 유효하지 않은 AI 응답으로 보고 Preview를 생성하지 않으며 Backend Job을 `FAILED`로 처리한다. Callback HTTP 응답은 `422 INVALID_AI_RESPONSE` 계열 검증 실패로 반환한다.
- `reason`은 공백 제외 최소 10자, 최대 100자다.
- HTML·Script를 이스케이프 처리한다.
- `requestType=TRIP_WISHLIST`이면 Callback 최종 결과에 `existingSchedule`의 모든 기존 장소와 `wishlistPlaceIds`의 모든 필수 위시리스트 장소가 포함되었는지 검증한다.
- `allowAdditionalRecommendations=false`인 `TRIP_WISHLIST` 결과에서는 기존 일정 장소와 필수 위시리스트 이외의 새 보완 장소가 추가되지 않았는지 검증한다.
- AI가 기존 장소의 Day 또는 방문 순서를 변경하는 것은 허용하며, Spring Boot는 기존 일정과 신규 추천을 다시 merge하지 않는다.
- 검증 실패 시 preview를 생성하지 않고 job을 `FAILED`로 변경한다.
- 검증 성공 시 Preview, PreviewDay, PreviewPlace와 job 상태를 하나의 트랜잭션으로 저장한다.
- 신규 Preview는 `retentionStatus=TEMPORARY`, `expiresAt=createdAt+24시간`, `savedAt=null`로 저장한다.
- TEMPORARY Preview가 만료되면 Preview의 경로·텍스트 콘텐츠를 삭제하고 최소 tombstone과 AI Job 기록만 유지한다.
- 경로 저장 시 Preview·PreviewDay·PreviewPlace를 복사하지 않고 동일 Preview의 `retentionStatus=PERMANENT`, `expiresAt=null`, `savedAt=현재시각`으로 변경한다.
- 영구 저장된 Preview는 저장 경로의 원본 `Source Data` 역할을 하며 `SavedRoute`가 해당 `previewId`를 계속 참조한다.
- 이후 사용자가 저장된 AI 경로를 수정하면 동일 Preview·PreviewDay·PreviewPlace를 갱신하며 별도 복사본은 생성하지 않는다.
- 이 단계에서는 Trip을 생성하거나 수정하지 않는다.

#### COMPLETED Callback Request DTO

- `jobId: Long` — required
- `status: COMPLETED` — required
- `tripTitle: String` — required
- `summary: String` — required
- `days: List<AiResultDay>` — required
  - `dayNumber: Int`
  - `places: List<AiResultPlace>`
    - `placeId: Long`
    - `order: Int`
    - `reason: String`
- `requestId: String?`와 `schemaVersion: String?`은 Optional 필드로 받으며 누락되어도 AI 결과를 거부하지 않는다.
- Callback 결과의 작업 식별에는 백엔드가 발급한 동일 `jobId: Long`을 필수값으로 사용한다.
- `recommendedTime`, `durationMinutes`, `plannedTime` 등 방문 시각·체류시간 필드는 Callback Schema에 포함하지 않는다.

#### FAILED Callback Request DTO

- 성공과 실패는 동일 Callback endpoint를 사용하며 `status`로 구분한다.
- DTO명은 `AiRecommendationFailedCallbackRequest`다.
- `jobId: Long` — required
- `status: FAILED` — required
- `code: String` — required
- `message: String` — required
- `timestamp: LocalDateTime` — required
- `details: Object?` — optional+nullable
- FAILED Callback은 공통 `ErrorResponse`의 `code`, `message`, `timestamp`, `details` 구조에 `jobId`, `status`를 결합한 flat 구조를 사용한다.
- AI 내부의 `{error:{...}}` 중첩 ErrorBody를 FAILED Callback Body로 사용하지 않는다.

#### 출력 / 처리 결과

- `COMPLETED` 결과 검증 성공 시 `previewId`를 생성하고 Backend AI 작업 상태를 `SUCCEEDED`로 변경한다.
- `FAILED` Callback을 정상 수신하면 Backend AI 작업 상태를 `FAILED`로 변경하고 Preview를 생성하지 않는다.
- 미리보기의 기본 보관 상태는 `TEMPORARY`다.
- `expiresAt`은 생성 시각부터 24시간 후다.
- 사용자가 경로 저장을 완료하면 동일 `previewId`가 `PERMANENT` 상태로 전환된다.
- 영구 전환 이후에도 동일 `previewId`를 대상으로 수정 API를 사용할 수 있다.

#### 예외사항 및 검증 로직

- 구조 오류는 `INVALID_AI_RESPONSE`다.
- 내부 장소 없음은 `AI_PLACE_NOT_FOUND`다.
- `placeId` 누락은 `AI_PLACE_ID_REQUIRED`다.
- 순서 오류는 `AI_PLACE_ORDER_INVALID`다.
- Day별 장소 수 15개 초과는 전체 결과 검증 실패로 처리한다.
- 전체 `days`에서 동일 `placeId` 중복은 `422 INVALID_AI_RESPONSE`로 처리하고 Preview를 생성하지 않는다.
- 추천 이유 누락·길이 오류는 `AI_RECOMMENDATION_REASON_INVALID`다.
- 기존 장소 또는 `TRIP_WISHLIST` 필수 장소 누락, 추가 추천 금지 정책 위반 등 계약 검증 실패는 전체를 `AI_RESULT_VALIDATION_FAILED`로 처리한다.
- 일부 오류가 있어도 유효 부분만 저장하지 않고 전체를 실패 처리한다.

#### AI 내부 오류 코드와 외부 오류 응답

- AI 서버는 내부 요청 검증·추천 처리 식별을 위해 `INVALID_REQUEST`, `PLACE_NOT_FOUND`, `PLACE_REGION_MISMATCH`, `CONFLICTING_PLACE_POLICY` 등의 내부 error code를 사용할 수 있다.
- `POST /internal/v1/recommendations` 즉시 응답에서는 `400 INVALID_REQUEST`, `422 PLACE_NOT_FOUND`, `422 PLACE_REGION_MISMATCH`, `422 CONFLICTING_PLACE_POLICY`로 역할을 구분한다.
- 동일 `jobId` 충돌은 별도 `409 AI_JOB_ID_CONFLICT`, Internal API Key 인증 실패는 `401 INVALID_INTERNAL_AI_KEY`를 사용한다.
- `400/409/422` 즉시 실패 Body는 프로젝트 공통 `ErrorResponse{code, message, timestamp, details}`를 사용하며 AI 전용 `{error:{...}}` 중첩 ErrorBody를 사용하지 않는다.
- AI 내부 HTTP 오류 Body를 프론트엔드에 그대로 노출하지 않으며 Spring Boot 공개 API는 프로젝트 공통 `ErrorResponse` 계약에 맞춰 필요한 공개 오류로 매핑한다.
- AI 내부 `400/422` 검증은 방어적 검증이며, 사용자 소유권·저장 장소 관계·Trip 위시리스트·필수 배치 장소 수용량 등 Backend가 알고 있는 정책은 Spring Boot가 AI 호출 전에 우선 검증한다.
- FAILED Callback의 `code`에는 AI 작업 실패 원인을 식별하는 code를 전달할 수 있으며 Callback Body 자체는 `3.19.5`의 flat 실패 DTO를 따른다.
- 방문 시각·체류시간 기능이 MVP에서 제거되었으므로 시간·체류시간 전용 AI 검증 오류코드는 사용하지 않는다.

#### 내부 연동 방식 및 Backend↔AI 인증·운영 파라미터

- 비동기 Callback 방식으로 확정한다.
- 백엔드는 AI 서버에 HTTP REST로 작업 요청을 전달하되 생성 결과 응답을 같은 연결에서 기다리지 않는다.
- Backend → AI 런타임 장소 동기화는 `PUT /internal/v1/places/sync`를 사용한다. AI는 `placeId`를 key로 canonical 장소 데이터를 멱등 upsert하고 성공 시 `204 No Content`를 반환한다. 이 API는 NAVER 전체를 GENERAL 추천 후보 풀로 확장하는 기능이 아니라, 사용자가 실제 저장·위시리스트·기존 일정에서 참조한 런타임 신규 장소를 AI가 해석할 수 있도록 보완하는 내부 동기화다.
- `PUT /internal/v1/places/sync`도 동일한 `X-Internal-AI-Key: {TRAVELBIRD_AI_CALLBACK_KEY}` Header를 필수로 사용한다. `400 INVALID_REQUEST`, `401 INVALID_INTERNAL_AI_KEY`는 공통 `ErrorResponse`를 사용한다.
- Spring Boot → AI `POST /internal/v1/recommendations`와 AI → Spring Boot `POST /internal/ai-callbacks/trip-recommendations` 모두 동일한 `X-Internal-AI-Key: {TRAVELBIRD_AI_CALLBACK_KEY}` Header를 필수로 사용한다.
- 양방향 모두 Header가 없거나 서버 설정의 Key와 일치하지 않으면 `401 INVALID_INTERNAL_AI_KEY`를 반환하고 요청 Body를 처리하지 않는다.
- Backend → AI `POST /internal/v1/recommendations`의 즉시 응답은 `202 | 400 | 401 | 409 | 422`를 사용한다. `202`는 정상 접수 또는 동일 `jobId`+동일 Payload 재전송의 멱등 접수, `400`은 `INVALID_REQUEST`, `409`는 `AI_JOB_ID_CONFLICT`, `422`는 `PLACE_NOT_FOUND | PLACE_REGION_MISMATCH | CONFLICTING_PLACE_POLICY`다.
- 동일 `jobId`+동일 Payload 재전송 시 AI 서버는 새 작업을 만들지 않고 기존 접수 결과를 `202 Accepted`로 반환한다. 동일 `jobId`+다른 Payload는 `409 AI_JOB_ID_CONFLICT`로 거부한다.
- 동일 `jobId`의 동일 Payload 여부는 `jobId`를 제외한 `RecommendationJobRequest`의 정규화된 의미 Payload를 SHA-256 fingerprint로 비교하여 판단한다. JSON 객체 Key 순서, 공백, 개행은 무시한다. 순서가 의미 없는 `themes`, `savedPlaceIds`, `wishlistPlaceIds`는 정렬하여 비교하고, `existingSchedule`은 `dayNumber` 기준으로 정규화하되 각 Day의 `placeIds` 순서는 유지한다. 동일 fingerprint면 기존 접수 결과를 `202 Accepted`로 멱등 반환하고, 다르면 `409 AI_JOB_ID_CONFLICT`를 반환한다.
- 즉시 실패 `400/409/422`는 공통 `ErrorResponse`를 사용한다. 이 중 `400/422`는 AI 서버의 방어적 검증이며 Backend가 알고 있는 사용자·저장 장소·위시리스트·수용량 정책은 Spring Boot가 호출 전에 먼저 검증한다.
- Spring Boot → AI의 Base URL은 `AI_SERVER_BASE_URL`, AI → Spring Boot Callback의 Base URL은 `BACKEND_CALLBACK_BASE_URL` 환경변수로 관리하며 코드에 주소를 하드코딩하지 않는다.
- `AI_SERVER_BASE_URL`, `BACKEND_CALLBACK_BASE_URL`, `TRAVELBIRD_AI_CALLBACK_KEY`는 모두 `local/dev/prod` 환경별로 분리한다.
- AI 서버는 작업을 완료하거나 실패한 뒤 `POST /internal/ai-callbacks/trip-recommendations`를 호출한다.
- Callback timeout은 AI `202 Accepted`를 확인하여 Backend Job이 `PROCESSING`으로 전환된 시점부터 **180초**다. 180초 동안 `COMPLETED` 또는 `FAILED` Callback이 없으면 Backend는 Job을 `FAILED`로 변경하고 `AI_CALLBACK_TIMEOUT`을 기록한다.
- AI → Backend Callback 전송 실패 시 최초 전송 이후 **최대 3회 재시도**한다. 재시도 대기시간은 순서대로 `2초`, `5초`, `10초`다.
- Callback 재시도 대상은 Network error, HTTP `408`, `429`, `5xx`다. HTTP `400`, `401`, `403`, `404`, `422`에는 재시도하지 않으며, 재시도 대상에 명시되지 않은 다른 HTTP 응답도 재시도하지 않는다.
- Callback이 `200 OK`를 받으면 성공적으로 전달된 것으로 보고 추가 재시도를 수행하지 않는다.
- `[채택·잔여 리스크]` 정적 Internal API Key는 MVP에서 채택한다. 실제 Key는 문서·소스·Git·로그에 기록하지 않고 32자 이상의 랜덤 영문 대소문자+숫자 Secret으로 배포 환경에서 생성·보관하며 HTTPS, `local/dev/prod` 환경별 Key 분리, 노출 시 교체를 적용한다.
- `requestId`, `schemaVersion`은 Optional이다.
- 동일 `jobId`의 동일 종료 결과 Callback이 재전송되어도 Preview나 실패 기록이 중복 생성되지 않도록 멱등 처리한다.
- `AI_CALLBACK_TIMEOUT`으로 Backend job이 이미 `FAILED`인 상태에서 늦은 `COMPLETED` Callback이 도착하면 기존 `FAILED` 상태를 유지하고 Preview를 생성하지 않으며 `200 OK`로 멱등 처리한다.
- Callback의 `jobId`가 존재하지 않으면 결과를 저장하지 않는다.
- `EXPIRED` 작업에는 새 결과를 저장하지 않는다.

#### 내부 Callback API 응답

- COMPLETED 결과 수신·검증·Preview 저장 성공: `200 OK`
  - `accepted: true`
  - `jobId: Long`
  - `previewId: Long`
  - `processedAt: LocalDateTime`
- FAILED Callback 정상 수신 및 Backend Job 실패 반영 성공: `200 OK`
  - 이 `200`은 AI 추천 성공이 아니라 실패 사실을 Spring Boot가 정상 수신·반영했다는 의미다.
- 동일 `jobId`의 동일 종료 결과 중복 Callback: 멱등 `200 OK`
- `AI_CALLBACK_TIMEOUT` 이후 늦게 도착한 `COMPLETED` Callback: 기존 Backend `FAILED` 상태 유지, Preview 생성 없음, 멱등 `200 OK`
- 인증 Header 누락·불일치: `401 INVALID_INTERNAL_AI_KEY`
- 존재하지 않는 작업: `404 AI_JOB_NOT_FOUND`
- COMPLETED 구조·검증 실패: `422 INVALID_AI_RESPONSE`

---

### 3.19.6 기능명: AI 일정 미리보기 조회·수정

#### 기능 설명

- 사용자는 자신에게 발급된 AI 일정 미리보기를 조회하고 내용을 수정할 수 있다.
- `TEMPORARY` 미리보기는 생성 후 24시간 내에만 조회·수정할 수 있다.
- `PERMANENT`로 경로 저장된 미리보기는 24시간 제한 없이 계속 조회·수정할 수 있다.
- 경로 저장 이후에도 수정 가능하며, 저장은 현재 내용을 고정하거나 편집을 잠그는 기능이 아니다.
- 저장된 AI 경로를 수정하면 해당 경로를 참조하는 저장 목록과 상세 화면에 변경사항이 즉시 반영된다.

#### 세부 로직 및 상태 변화

- JWT 사용자 ID와 Preview 소유자 ID를 비교하여 본인 미리보기인지 검증한다.
- 미리보기의 `retentionStatus`와 `expiresAt`을 확인한다.
- `TEMPORARY`이면서 만료되지 않은 미리보기와 `PERMANENT` 미리보기만 수정할 수 있다.
- 사용자는 다음 항목을 수정할 수 있다.
  - 일정 제목 `tripTitle`
  - 일정 설명 `summary`
  - 해시태그 `hashtags`
  - Day별 장소 추가·삭제
  - 장소의 Day 이동
  - Day 안의 장소 방문 순서 변경
- 지역, 여행 시작일·종료일, 동행 유형, 테마와 일정 밀도처럼 AI 추천의 입력조건 자체는 Preview에서 수정하지 않는다. 변경하려면 새로운 AI 추천 요청을 생성한다.
- 경로 전체를 수정하는 경우 클라이언트는 최종 Day와 장소 목록 전체를 전송하며 서버는 하나의 트랜잭션으로 반영한다.
- 장소 추가 시 내부 `placeId`가 존재하는지 검증한다.
- 동일한 `placeId`는 하나의 Preview 전체에서 최대 한 번만 포함할 수 있다. 같은 장소를 같은 Day뿐 아니라 서로 다른 Day에도 중복 배치할 수 없다. 장소를 다른 Day로 이동하거나 Day 안에서 순서를 변경하는 것은 허용한다.
- Day별 장소 수는 최대 15개다.
- Day별 `order`는 1부터 연속되도록 재정렬한다.
- 수정 시 기존 `SavedRoute` 관계와 `previewId`는 유지한다.
- 수정 완료 후 `updatedAt`과 `version`을 갱신하고, 최초 경로 저장 시각인 `savedAt`은 변경하지 않는다.
- 수정된 Preview가 이미 `SavedRoute`에 연결되어 있으면 별도의 저장 요청 없이 변경사항이 저장된 경로에 즉시 반영된다.
- Preview를 기반으로 이미 생성된 Trip은 별도 객체이므로 Preview 수정사항을 자동 반영하지 않는다. 변경된 경로로 Trip을 다시 만들거나 기존 Trip에 적용하려면 사용자의 별도 확정 동작이 필요하다.
- 동시 수정 충돌 방지를 위해 `version`을 이용한 낙관적 락을 적용한다.

#### 입력 / Request DTO

- 조회
  - Path `previewId: Long`
- 수정 DTO명은 `UpdateAiTripPreviewRequest`다.
  - `tripTitle: String` — optional, 전달 시 non-null
  - `summary: String` — optional, 전달 시 non-null
  - `hashtags: List<String>` — optional·non-null, `[]`이면 해시태그 전체 제거
  - `days: List<UpdateAiPreviewDayRequest>` — optional·non-null
    - `day: Int`
    - `places: List<UpdateAiPreviewPlaceRequest>`
      - `placeId: Long`
      - `order: Int`
  - `version: Long` — required
- `days`가 전달되면 현재 경로의 최종 Day·장소 구성을 전체 목록으로 전달한다.

#### 출력 / Response DTO

- `previewId: Long`
- `retentionStatus: TEMPORARY | PERMANENT`
- `editable: Boolean`
- `tripTitle: String`
- `summary: String`
- `hashtags: List<String>`
- `days: List<AiPreviewDayResponse>`
- `createdAt: LocalDateTime`
- `updatedAt: LocalDateTime`
- `expiresAt: LocalDateTime?`
- `savedAt: LocalDateTime?`
- `version: Long`

#### 예외사항 및 검증 로직

- 비로그인 요청은 `401 UNAUTHORIZED`다.
- 존재하지 않는 미리보기는 `404 AI_PREVIEW_NOT_FOUND`다.
- 다른 사용자의 미리보기는 `403 AI_PREVIEW_ACCESS_DENIED`다.
- 만료된 임시 미리보기는 상세 콘텐츠가 삭제된 상태이므로 `410 AI_PREVIEW_EXPIRED`다.
- 존재하지 않는 장소는 `404 PLACE_NOT_FOUND`다.
- Preview 전체에서 동일 `placeId`가 둘 이상 존재하면 `409 AI_PREVIEW_PLACE_DUPLICATED`다.
- Day별 장소 수가 15개를 초과하면 `422 AI_PREVIEW_DAY_PLACE_LIMIT_EXCEEDED`다.
- Day 번호나 장소 순서가 올바르지 않으면 `400 INVALID_AI_PREVIEW_ROUTE`다.
- 버전 충돌은 `409 AI_PREVIEW_MODIFICATION_CONFLICT`다.

#### 연동 API 엔드포인트

- `GET /api/ai/trip-previews/{previewId}`
- `PATCH /api/ai/trip-previews/{previewId}`

---

### 3.19.7 기능명: AI 일정 미리보기 확정 적용

#### 기능 설명

- 사용자는 AI 미리보기를 실제 Trip으로 새로 생성하거나, 위시리스트 기반 미리보기인 경우 원본 Trip에 확정 적용한다.
- 일반 조건 및 저장 장소 기반 미리보기는 새 Trip을 생성한다.
- `TRIP_WISHLIST` 미리보기는 연결된 기존 Trip의 Day와 장소 구성을 갱신한다.

#### 세부 로직 및 상태 변화

- Preview 소유권, 만료 여부와 검증 완료 상태를 확인한다.
- `GENERAL`, `SAVED_PLACES` Preview는 `Trip`, `TripDay`, `TripPlace`를 하나의 트랜잭션으로 생성한다.
- `TRIP_WISHLIST` Preview는 연결된 Trip 소유권과 경로 수정 가능 여부를 다시 검증한 후 기존 Day·장소에 적용한다.
- `TRIP_WISHLIST` 적용 시 `existingSchedule`에 있던 기존 장소는 유지 대상이므로 해당 기존 `TripPlace`의 메모·사진 등 연결 콘텐츠를 삭제하거나 초기화하지 않고 최종 Day/order 재배치만 반영한다. AI가 새로 추가한 장소만 새로운 `TripPlace`로 생성한다.
- 적용되는 최종 Preview는 Trip 전체에서 동일 `placeId`가 한 번만 존재해야 하며, 기존 장소를 다른 Day로 이동시키는 경우 기존 `TripPlace`를 재사용해 Day/order만 변경한다. 동일 장소의 복제 `TripPlace`를 새로 만들지 않는다.
- 연결 Trip의 `cancelledAt`이 존재하면 적용을 거부한다.
- 기존 Trip에 발행된 Post가 연결되어 있으면 AI 결과를 적용하지 않고 `409 TRIP_ROUTE_LOCKED_BY_PUBLISHED_POST`를 반환한다.
- 동일 Preview를 중복 적용하면 기존 적용 결과 `tripId`를 반환하도록 멱등 처리한다.
- Preview를 Trip에 적용해도 Preview의 TEMPORARY·PERMANENT 보관 상태는 별도 정책에 따른다.

#### 입력 / Request DTO

- Path `previewId: Long`
- 별도 Body 없음

#### 출력 / Response DTO

- `tripId: Long`
- `applyType: CREATED_NEW_TRIP | UPDATED_EXISTING_TRIP`
- `appliedAt: LocalDateTime`

#### 예외사항 및 검증 로직

- 비로그인 요청은 `401 UNAUTHORIZED`다.
- 존재하지 않는 Preview는 `404 AI_PREVIEW_NOT_FOUND`다.
- 만료된 Preview는 `410 AI_PREVIEW_EXPIRED`다.
- 타인 Preview는 `403 AI_PREVIEW_ACCESS_DENIED`다.
- 기존 Trip 경로 잠금은 `409 TRIP_ROUTE_LOCKED_BY_PUBLISHED_POST`다.
- 취소된 기존 Trip은 `409 TRIP_CANCELLED_READ_ONLY`다.

#### 연동 API 엔드포인트

- `POST /api/ai/trip-previews/{previewId}/apply`

---

### 3.19.8 기능명: AI 일정 구성 프롬프트 공통 규칙

#### 기능 설명

- `pace`, `existingSchedule`, `SAVED_PLACES`, `TRIP_WISHLIST`, `allowAdditionalRecommendations`의 의미가 AI 구현에서 달라지지 않도록 Backend→AI 프롬프트 공통 규칙을 고정한다.
- 이 규칙은 API Schema의 대체물이 아니라, 확정된 API 계약을 AI가 일정 생성 시 해석하는 공통 지침이다.

#### AI 프롬프트 규칙

```text
[TravelBird 일정 밀도 및 전체 경로 구성 규칙]

1. pace는 하루 장소 개수를 고정하는 숫자 제한이 아니다.
   RELAXED/NORMAL/DENSE는 일정 전체의 밀도와 이동 강도를 조절하기 위한 성향 값이다.

2. RELAXED
   - 장소 수를 과도하게 늘리지 않는다.
   - 이동 거리가 지나치게 길어지지 않도록 같은 권역의 장소를 우선 배치한다.
   - 불필요한 왕복 이동을 최소화한다.
   - 일정에 여유가 있는 구성을 우선한다.

3. NORMAL
   - 장소 수, 이동 거리, 다양한 관광 경험 사이의 균형을 우선한다.
   - 한 Day가 지나치게 비거나 지나치게 빽빽하지 않도록 구성한다.
   - 같은 권역의 장소를 우선 연결하되 일정 다양성도 고려한다.

4. DENSE
   - RELAXED/NORMAL보다 더 많은 장소와 다양한 경험을 포함하는 방향으로 구성한다.
   - 단, 이동 동선이 비현실적으로 길어지거나 반복 왕복하지 않도록 한다.
   - 단순히 장소 수를 늘리는 것보다 효율적인 경로 구성을 우선한다.

5. pace에 따라 하루 최대 장소 수를 2/3/4처럼 고정하지 않는다.
   최종 장소 수는 여행 기간, 기존 일정, 필수 장소, 후보 장소의 위치와 추천 적합도를
   종합적으로 고려해 AI가 판단한다.

6. existingSchedule에 포함된 장소는 최종 결과에서 제거하지 않는다.
   단, 기존 dayNumber와 방문 순서는 고정값이 아니라 현재 상태를 나타내는 참고 정보다.

7. TRIP_WISHLIST의 wishlistPlaceIds는 모두 최종 일정에 반드시 포함한다.

8. SAVED_PLACES의 savedPlaceIds는 반드시 포함하는 장소가 아니다.
   사용자의 선호 신호로 활용하여 추천 우선순위에 반영한다.

9. allowAdditionalRecommendations=true이면 기존 장소와 필수 장소 외에
   적절한 추가 장소를 추천할 수 있다.

10. allowAdditionalRecommendations=false이면 새로운 보완 장소는 추가하지 않는다.
    단, existingSchedule 장소나 TRIP_WISHLIST 필수 장소를 제거해서는 안 된다.

11. AI는 신규 추천 장소만 반환하지 않는다.
    기존 일정 장소, 필수 위시리스트 장소, 추가 추천 장소를 포함한
    최종 전체 Day별 일정과 방문 순서를 반환한다.

12. 기존 장소도 필요하면 다른 Day로 이동시키거나 Day 안에서 순서를 변경할 수 있다.

13. 새로 추천하는 장소는 전달받은 regionCode의 지역 범위 안에서 선정한다.

14. 존재하지 않는 placeId를 생성하지 않는다.
    Backend/AI 데이터에 존재하는 내부 placeId만 사용한다.

15. recommendedTime, durationMinutes, plannedTime 등 시간/체류시간 필드는 생성하지 않는다.

16. 하나의 최종 일정 전체에서 동일한 placeId를 두 번 이상 배치하지 않는다.
    같은 장소를 서로 다른 Day에 반복 배치하는 것도 금지한다.
    existingSchedule의 기존 장소를 다른 Day로 이동하거나 방문 순서를 바꾸는 것은 허용하지만, 최종 결과에는 해당 placeId가 정확히 한 번만 존재해야 한다.

17. Backend가 전달한 existingSchedule과 wishlistPlaceIds의 필수 장소는 모두 유지하되, Day별 시스템 최대 15개를 준수한다.
    Backend가 필수 장소 총수가 여행 Day 수 × 15를 초과하는 요청은 AI 호출 전에 거부하므로, AI는 수용 불가능한 필수 장소 삭제로 문제를 해결하려고 시도하지 않는다.

18. 최종 결과는 각 Day마다 placeId와 order를 명확히 반환한다.
    recommendation reason이 필요한 경우 기존 계약의 reason 규칙을 따른다.
```

- 현재 Backend AI Result 계약에서는 `reason`이 필수이므로 모든 반환 장소는 `3.19.5`의 `reason` 검증 규칙을 충족해야 한다.
- 위 프롬프트의 `2/3/4`는 “고정 장소 수 정책을 사용하지 않는다”는 설명에만 등장하며 실제 API 계약값이나 Day별 최대 장소 수가 아니다.

---


### 3.19.9 기능명: AI 추천 전 런타임 장소 데이터 동기화

#### 기능 설명

- 초기 TourAPI 데이터에 없던 NAVER 기반 canonical 장소가 저장 장소·위시리스트·기존 일정에 포함되어도 AI가 해당 `placeId`를 해석할 수 있도록 추천 작업 전달 전에 필요한 장소 데이터를 동기화한다.
- RecommendationJobRequest와 Callback의 장소 식별자 계약은 기존과 동일하게 `placeId: Long`만 사용한다.

#### Backend → AI 동기화 Request

- Endpoint: `PUT /internal/v1/places/sync`
- Header: `X-Internal-AI-Key: {TRAVELBIRD_AI_CALLBACK_KEY}`
- Body: `AiPlaceSyncRequest`
  - `places: List<AiPlaceSyncItem>` — 1개 이상
  - 각 item: `placeId: Long`, `regionCode: String`, `name: String`, `address: String`, `latitude: BigDecimal`, `longitude: BigDecimal`, `category: PlaceCategory`
- AI 서버는 `placeId`를 기준으로 멱등 upsert한다. 같은 `placeId`가 다시 오면 새 장소를 중복 생성하지 않고 Backend가 전달한 canonical 정보를 최신값으로 반영한다.

#### 처리 결과

- 성공: `204 No Content`
- 요청 구조/필드 오류: `400 INVALID_REQUEST` + 공통 `ErrorResponse`
- Internal Key 누락/불일치: `401 INVALID_INTERNAL_AI_KEY` + 공통 `ErrorResponse`
- 동기화 실패 시 해당 Backend AI job은 `FAILED` + `AI_PLACE_SYNC_FAILED`로 기록하고 `/internal/v1/recommendations`를 호출하지 않는다.
- 이 동기화 실패는 사용자가 이미 저장한 장소나 Trip 자체를 롤백하지 않는다. 장소 도메인의 canonical `placeId`가 기준 데이터의 Source of Truth다.

#### 책임 경계

- AI/관광데이터: TourAPI 사전 수집·정규화, 초기 동일 장소 후보 추출 지원, Backend가 전달한 `placeId`를 추천 데이터에 반영한다.
- Backend: `places`/`place_external_ids` 저장, 최종 동일 장소 병합 판단, `placeId` 발급, 추천 전 런타임 NAVER-only 장소 동기화 책임을 가진다.
- Frontend: 기존 `externalPlaceId → /api/places/resolve → placeId` 흐름만 사용하며 `place_external_ids`나 내부 sync API를 알 필요가 없다.

---

# 4. 결정사항 반영 및 v10 검증 결과

## 4.1 v10 변경 표시 적용 범위

- 기존 v10은 v9 재검증에서 발견된 2개 보완사항을 확정 정책으로 반영했다.
  1. AI 호출 전 필수 배치 장소 수용량 검증 및 `422 AI_ROUTE_REQUIRED_PLACE_LIMIT_EXCEEDED`
  2. 하나의 Trip/AI Preview 전체에서 동일 `placeId` 중복 배치 금지
- 이번 운영 파라미터 확정에서는 기존 v10 계약을 유지한 채 다음 3개 운영·배포 기준을 추가 확정했다.
  1. Callback timeout `180초` — Backend Job `PROCESSING` 전환 시점부터 계산
  2. Callback 실패 시 최대 3회 재시도 — Backoff `2초 → 5초 → 10초`, Network error·`408`·`429`·`5xx`만 재시도
  3. `AI_SERVER_BASE_URL`, `BACKEND_CALLBACK_BASE_URL`, `TRAVELBIRD_AI_CALLBACK_KEY`를 코드에 하드코딩하지 않고 `local/dev/prod` 환경별로 분리 관리
- v9까지 확정된 Backend↔AI 비동기 연동, `existingSchedule` 전체 재구성, 기존 장소 항상 KEEP, `TRIP_WISHLIST` MUST_INCLUDE, `SAVED_PLACES` 선호 참고, Callback 인증·FAILED DTO, AI 4상태/Backend 5상태, `pace` 정성적 정책은 그대로 유지한다.
- 최신 결정과 충돌하는 v8/v9 구버전 표현은 유지하지 않고 v10 정책으로 교체한다.

## 4.2 v10 신규 보완 2건 반영 검증

1. **필수 배치 장소 수용량 사전 검증**
   - **반영 완료.** Spring Boot는 `TRIP_WISHLIST` 요청에서 AI 호출 전에 `count(distinct(existingSchedule placeIds ∪ wishlistPlaceIds))`를 계산한다.
   - 필수 배치 장소 수가 `여행 Day 수 × 15`를 초과하면 AI 서버를 호출하지 않고 `422 AI_ROUTE_REQUIRED_PLACE_LIMIT_EXCEEDED`를 반환한다.
   - 이 검증은 `existingSchedule` 기존 장소 KEEP, `wishlistPlaceIds` MUST_INCLUDE, Day별 최대 15개라는 세 정책이 동시에 만족될 수 없는 요청을 사전에 차단한다.
   - `allowAdditionalRecommendations` 값과 무관하게 적용한다. `false`는 새 보완 장소만 막는 값이므로 필수 장소 수용량 초과를 해결하지 않는다.

2. **Trip/Preview 전체 동일 placeId 중복 금지**
   - **반영 완료.** 하나의 Trip 또는 AI Preview에서 동일한 `placeId`는 최대 한 번만 존재한다.
   - 같은 장소를 같은 Day뿐 아니라 서로 다른 Day에도 반복 배치할 수 없다.
   - 장소를 다른 Day로 이동하거나 같은 Day 안에서 방문 순서를 변경하는 것은 허용한다.
   - AI 결과 역시 전체 `days`를 기준으로 동일 `placeId`가 중복되면 유효하지 않은 결과로 처리하고 Preview를 생성하지 않는다.
   - AI Callback의 전체 일정에 동일 `placeId`가 중복되면 `422 INVALID_AI_RESPONSE` 계열 검증 실패로 처리하고 Backend Job을 `FAILED`로 전환한다.
   - 사용자가 Preview를 수정할 때도 전체 Preview 기준 중복을 검증하여 `409 AI_PREVIEW_PLACE_DUPLICATED`를 반환한다.
   - 직접 Trip 장소 추가도 Trip 전체 기준으로 중복을 검증하며 기존 `409 PLACE_ALREADY_ADDED`를 사용한다.

## 4.3 1차 검증: 폐기·충돌 표현 재검색

v10 기능 본문을 기준으로 다음 정책이 활성 계약으로 남아 있는지 재검사했다.

- `같은 장소를 다른 Day에는 추가할 수 있다`: **0건**
- `같은 장소는 동일 Day에만 중복 금지하고 다른 Day에는 허용`: **0건**
- Preview에서 동일 장소의 다른 Day 중복 허용: **0건**
- AI 결과에서 같은 `placeId`를 서로 다른 Day에 반복 배치 허용: **0건**
- 수용량 검증 없이 필수 장소를 무조건 AI에 전달하는 TRIP_WISHLIST 정책: **0건**
- `existingPlaceMode: KEEP | REPLACE` 활성 사용: **0건**
- AI `scheduleDensity=RELAXED|NORMAL|FULL` 활성 사용: **0건**
- `RELAXED/NORMAL/DENSE = 2/3/4곳` 고정 계약: **0건**
- 기존 장소를 Callback 결과에서 제외하는 정책: **0건**
- `recommendedTime`, `durationMinutes`, `plannedTime` 활성 AI/Trip 시간 필드: **0건**

## 4.4 2차 검증: 최신 결정 존재 여부

- `Pace = RELAXED | NORMAL | DENSE`, 고정 장소 수 의미 없음: **확인**
- `placeId`, `jobId = Long/int64 → JSON number`: **확인**
- 단일 5자리 `regionCode`: **확인**
- `startDate`, `endDate` 기반 AI 내부 요청: **확인**
- `TravelTheme` 6개, `CompanionType` 5개: **확인**
- `requestType = GENERAL | SAVED_PLACES | TRIP_WISHLIST`: **확인**
- `SAVED_PLACES` = 선호 참고 / `TRIP_WISHLIST` = MUST_INCLUDE: **확인**
- Spring Boot 생성 `existingSchedule`: **확인**
- 기존 일정 장소 항상 KEEP, Day/order 재구성 가능: **확인**
- `allowAdditionalRecommendations=false`여도 기존·필수 장소 유지: **확인**
- 필수 배치 장소 `> Day 수 × 15` 사전 `422 AI_ROUTE_REQUIRED_PLACE_LIMIT_EXCEEDED`: **확인**
- Trip 전체 동일 `placeId` 최대 1회: **확인**
- AI Preview 전체 동일 `placeId` 최대 1회: **확인**
- AI 전체 `days` 동일 `placeId` 중복 → `422 INVALID_AI_RESPONSE`: **확인**
- AI Callback 전체 일정 반환: **확인**
- `X-Internal-AI-Key` / `TRAVELBIRD_AI_CALLBACK_KEY`: **확인**
- FAILED Callback flat DTO 및 정상 수신 `200 OK`: **확인**
- AI 4상태 / Backend 5상태: **확인**
- 방문 시간·체류시간 MVP 삭제: **확인**

## 4.5 3차 검증: 기능 간 교차 일관성

- Trip 전체의 `placeId` 유일성 정책을 적용함으로써 `existingSchedule[{dayNumber, placeIds}]`만으로도 기존 장소를 식별할 때 같은 `placeId`가 여러 기존 TripPlace를 가리키는 모호성이 사라진다.
- 기존 장소의 Day/order를 변경할 수 있다는 정책과 동일 `placeId` 중복 금지는 충돌하지 않는다. 이동은 기존 장소 하나의 위치만 변경하는 것이며 복제 배치가 아니다.
- `TRIP_WISHLIST`의 기존 장소 KEEP + 위시리스트 MUST_INCLUDE와 Day별 최대 15개 제한은 사전 수용량 검증으로 연결되어 서로 충돌하지 않는다.
- `AI_ROUTE_REQUIRED_PLACE_LIMIT_EXCEEDED`는 AI 호출 전 사용자 요청 단계의 `422`이고, `INVALID_AI_RESPONSE`는 AI가 반환한 결과가 계약을 위반했을 때의 Callback 검증 오류이므로 역할이 겹치지 않는다.
- `allowAdditionalRecommendations=false`는 새 보완 장소만 금지하며 필수 배치 장소 수용량 검증과 독립적으로 적용된다.
- AI `pace`는 장소 수의 정성적 밀도 조건이고 Day별 최대 15개는 저장 가능한 시스템 하드 상한이므로 충돌하지 않는다.
- AI가 전체 일정을 반환하고 Spring Boot가 기존/신규 결과를 별도 merge하지 않는 정책은 유지된다. Backend는 전체 결과의 placeId 유일성·필수 포함·Day/order·장소 존재 여부를 검증한다.
- `TRIP_WISHLIST` Preview를 실제 Trip에 적용할 때 기존 장소는 기존 TripPlace를 재사용하여 Day/order만 변경하므로 메모·사진을 보존하면서 동일 placeId 복제도 발생하지 않는다.
- AI `COMPLETED`와 Backend `SUCCEEDED`는 여전히 다른 단계이며 v10 중복/필수용량 검증까지 통과하고 Preview 저장이 완료되어야 Backend `SUCCEEDED`가 된다.

## 4.6 최종 판정

- v9에서 요청받은 신규 보완사항: **2건**
- 기존 v10 반영 완료: **2/2**
  1. `422 AI_ROUTE_REQUIRED_PLACE_LIMIT_EXCEEDED` 사전 수용량 검증
  2. Trip/AI Preview 전체 동일 `placeId` 중복 금지 및 AI 결과 전역 중복 검증
- 이번 운영 파라미터 확정사항: **3건**
- 운영 파라미터 반영 완료: **3/3**
  1. Callback timeout `180초` 및 `PROCESSING` 전환 시점 기준
  2. Callback 최대 3회 재시도, Backoff `2초 → 5초 → 10초`, 재시도 대상 HTTP/Network 범위
  3. `AI_SERVER_BASE_URL`, `BACKEND_CALLBACK_BASE_URL`, Internal API Key의 `local/dev/prod` 환경별 분리 관리
- 이번 내부 AI 요청 즉시 응답 규격 확정: **1개 계약 묶음**
  - `202/400/401/409/422` 역할 분리
  - 동일 `jobId`+동일 Payload → 기존 접수 결과 `202` 멱등 반환
  - 동일 `jobId`+다른 Payload → `409 AI_JOB_ID_CONFLICT`
  - `400/409/422` → 공통 `ErrorResponse`, 별도 중첩 `ErrorBody` 없음
  - AI `400/422`는 방어적 검증, Backend 정책은 AI 호출 전에 우선 검증
- 내부 AI 즉시 응답 규격 반영 완료: **1/1**
- 최신 확정사항 기준 미반영 항목: **0건**
- v8/v9 구버전 정책이 최신 결정을 막아 유지된 항목: **0건**
- v10 기능 본문에서 발견된 활성 상호 모순: **0건**
- `existingSchedule` 식별 모호성: **Trip 전체 placeId 유일성 정책으로 해소**
- 필수 장소 수가 시스템 최대 수용량을 초과하는 경계조건: **사전 422 검증으로 해소**
- AI 프롬프트의 추천 품질·튜닝 적절성은 이전 결정대로 검증 대상에서 제외했다.
- 이번 검증은 기능명세서·DTO·상태 전이·Backend↔AI 계약의 정적 일관성 검증이며 실제 Spring Boot↔AI 서버 통합 테스트를 수행한 것은 아니다.

## 4.7 v10 기준 기능명세서·OpenAPI 후속 수정 체크포인트

향후 Spring Boot OpenAPI와 AI OpenAPI를 실제 수정할 때는 아래 항목을 v10 계약 기준으로 맞춘다.

| 구버전/현재 초안 표현 | v10 최종 기준 |
| --- | --- |
| 동일 `placeId`를 서로 다른 Day에 중복 배치 가능 | 하나의 Trip/Preview 전체에서 동일 `placeId` 최대 1회 |
| AI 결과에서 Day별 중복만 검사 | 전체 `days` 기준 동일 `placeId` 중복 검사, 위반 시 `422 INVALID_AI_RESPONSE` |
| 필수 장소 수용량 초과를 AI에게 전달 | AI 호출 전 `requiredPlaceCount > dayCount × 15`이면 `422 AI_ROUTE_REQUIRED_PLACE_LIMIT_EXCEEDED` |
| AI `jobId: string` | `jobId: integer/int64` |
| AI `regionCode: SEOUL` 등 자체 지역 문자열 | 5자리 시군구 코드 `String` 한 개 |
| AI `travelDays` 원본 입력 | `startDate`, `endDate` |
| `scheduleDensity` | `pace` |
| `RELAXED | NORMAL | FULL` | `RELAXED | NORMAL | DENSE` |
| `RELAXED=2 / NORMAL=3 / FULL=4` | 고정 장소 수 의미 삭제, 프롬프트의 정성적 밀도 규칙 적용 |
| 자유 문자열 `themeTags` | `themes: List<TravelTheme>` 1~3개 |
| `existingSchedule` 장소를 결과에서 제외 | 기존 장소 모두 유지 + AI가 전체 Day/order 재구성 + 전체 일정 Callback |
| `existingPlaceMode: KEEP | REPLACE` | MVP 제거, 기존 장소 항상 KEEP |
| `wishlistPlaceIds` 단순 가중치 | `TRIP_WISHLIST`에서는 전부 `MUST_INCLUDE` |
| `savedPlaceIds` 필수 포함 | 필수 포함 아님, 사용자 선호·추천 우선순위 신호 |
| `allowAdditionalRecommendations=false` 시 필수 장소까지 제외 가능 | 새 보완 장소만 금지, 기존 장소·필수 위시리스트는 유지 |
| 신규 장소만 Callback | 기존 장소 포함 최종 전체 Day/place/order Callback |
| Callback의 시간·체류시간 필드 | 제거 |
| Callback URL `/internal/v1/recommendation-jobs/{jobId}/result` | `POST /internal/ai-callbacks/trip-recommendations` |
| Callback 인증 미정 | `X-Internal-AI-Key: {TRAVELBIRD_AI_CALLBACK_KEY}` |
| FAILED Callback `{error:{...}}` 중첩 구조 | `jobId + status + code + message + timestamp + details` flat 구조 |
| AI Job/Backend Job 상태 강제 통일 | AI 4개, Backend 5개 유지 및 명시적 매핑 |
| 직접 일정 `pace: DENSE | RELAXED` | `pace: RELAXED | NORMAL | DENSE` |
| `KakaoLoginResponse.nickname`, `birdType` nullable이지만 required 누락 | 두 Key 모두 required+nullable |
| PATCH optional 필드에 일괄 nullable | optional과 nullable 분리, 실제 삭제 허용 필드만 nullable |
| Day 장소 추가 / 장소 메모·사진 수정의 Body 없는 `200` | `204 No Content` |
| `SAVED_PLACES.savedPlaceIds` nullable | `requestType=SAVED_PLACES`에서 최소 1개 조건부 필수·non-null |
| 여러 Error Code를 `"A / B / C"` 한 문자열로 예시 | 오류별 개별 OpenAPI `examples` |
| `AiJobStatusResponse.previewRetentionStatus` 항상 Enum 값 요구 | required+nullable, Preview 미생성 상태는 `null` |
| `AiJobStatusResponse.requestedAt` nullable | required·non-null |
| `PostDetailResponse.places`, `route`가 임의 Object | 표준 `PostDetailPlaceResponse`, `PostDetailRouteDay`로 구체화 |
| Backend → AI 내부 요청 인증 없음 | Callback과 동일한 `X-Internal-AI-Key` 적용 |
| `AI_CALLBACK_TIMEOUT` 후 늦은 COMPLETED 처리 미정 | 기존 FAILED 유지, Preview 생성 없음, `200 OK` 멱등 처리 |
| Callback timeout 숫자 미정 | Backend Job `PROCESSING` 전환 시점부터 `180초` |
| Callback retry/backoff 미정 | 실패 시 최대 3회, `2초 → 5초 → 10초`; Network error·`408`·`429`·`5xx`만 재시도 |
| 내부 서버 주소 코드/예시 의존 | `AI_SERVER_BASE_URL`, `BACKEND_CALLBACK_BASE_URL` 환경변수 사용, `local/dev/prod` 분리 |
| Internal API Key 환경 분리 불명확 | `TRAVELBIRD_AI_CALLBACK_KEY`를 `local/dev/prod`별로 분리 |
| `POST /internal/v1/recommendations` 즉시 응답이 `202/401`만 정의 | `202/400/401/409/422`로 역할 분리 |
| 동일 `jobId` 재전송 및 동일 Payload 판정 기준 | `jobId` 제외 `RecommendationJobRequest`를 정규화해 SHA-256 fingerprint 비교. JSON Key 순서·공백·개행 무시, `themes`/`savedPlaceIds`/`wishlistPlaceIds` 정렬, `existingSchedule`은 `dayNumber` 정렬·각 Day `placeIds` 순서 유지. 동일 fingerprint면 `202`, 다르면 `409 AI_JOB_ID_CONFLICT` |
| 내부 AI 즉시 오류에 별도/중첩 ErrorBody 사용 | `400/409/422` 모두 공통 `ErrorResponse{code,message,timestamp,details}` |
| Backend 정책 검증까지 AI `400/422`에 위임 | AI `400/422`는 방어적 검증, 소유권·저장 장소·위시리스트·수용량은 Backend가 AI 호출 전 검증 |

## 4.8 OpenAPI 최신 수정사항 기능명세서 반영 검증

### 반영한 수정사항

1. `KakaoLoginResponse.nickname`, `birdType`은 응답 Key가 항상 존재하므로 required로 명시하고 값은 nullable로 유지했다.
2. PATCH DTO의 optional과 nullable 의미를 분리했다. 미전달 가능한 필드와 `null` 허용 필드를 구분하고 배열은 `[]`로 비우도록 명시했다.
3. Day 장소 추가·삭제와 장소별 메모·사진 수정처럼 성공 Response Body가 없는 API는 `204 No Content`로 명시했다.
4. `SAVED_PLACES.savedPlaceIds`는 `requestType=SAVED_PLACES`에서 최소 1개 조건부 필수이며 non-null로 명시했다.
5. 여러 Error Code를 하나의 문자열로 합치지 않고 OpenAPI `examples`에서 개별 오류로 표현한다는 공통 규칙을 추가했다.
6. `AiJobStatusResponse.previewRetentionStatus`는 required+nullable로 정리하여 Preview 미생성 상태에서는 `null`을 반환하도록 했다.
7. `AiJobStatusResponse.requestedAt`은 Job 생성 시 항상 존재하므로 required·non-null로 명시했다.
8. `PostDetailResponse.places`를 표준 장소 필드 기반 `PostDetailPlaceResponse`로, `route`를 Day별 좌표 순서 목록인 `PostDetailRouteDay`로 구체화했다.
9. Spring Boot → AI `POST /internal/v1/recommendations`에도 Callback과 동일한 `X-Internal-AI-Key: {TRAVELBIRD_AI_CALLBACK_KEY}` 인증을 적용했다.
10. `AI_CALLBACK_TIMEOUT` 이후 늦은 `COMPLETED` Callback은 기존 Backend `FAILED` 상태를 유지하고 Preview를 생성하지 않으며 `200 OK`로 멱등 처리하도록 확정했다.
11. 폐기된 최대 여행기간·시간/체류시간 관련 Error Code와 구버전 AI 계약은 새로 추가하지 않았다.

### 정적 교차 검증 결과

- 최신 수정사항 기능명세서 반영: **11/11 완료**
- `PostDetailResponse`의 장소/경로 임의 Object 표현: **0건**
- Backend→AI 인증 미정 또는 무인증 활성 정책: **0건**
- `AI_CALLBACK_TIMEOUT` 이후 늦은 COMPLETED 처리 미정: **0건**
- `previewRetentionStatus`를 모든 Job 상태에서 강제로 non-null Enum으로 요구하는 활성 계약: **0건**
- `requestedAt` nullable 활성 계약: **0건**
- `SAVED_PLACES.savedPlaceIds` null 허용 활성 계약: **0건**
- Day 장소 추가·삭제 및 장소 메모·사진 수정의 Body 없는 `200 OK` 활성 계약: **0건**
- `TRIP_PERIOD_TOO_LONG`, `MULTIPLE_REGIONS_NOT_ALLOWED`, `INVALID_TRIP_PLACE_DURATION`, `INVALID_AI_PREVIEW_PLACE_SCHEDULE`, `INVALID_PLACE_RESOLVE_REQUEST` 활성 Error Code: **0건**
- `plannedTime`, `recommendedTime`, `durationMinutes`, `travelDays`, `scheduleDensity`, `existingPlaceMode` 활성 Request/Response 필드: **0건**
- 최신 기능명세서 내부에서 발견된 신규 상호 모순: **0건**

### 최종 판정

- 앞서 확정한 OpenAPI 수정사항과 본 기능명세서의 계약을 다시 일치시켰다.
- 이번 문서는 기능명세서·DTO·HTTP Status·Backend↔AI 인증·Callback 상태 전이를 기준으로 정적 검증한 최종안이다.
- 실제 Spring Boot↔AI 서버 통합 테스트와 Swagger 런타임 검증은 구현 단계에서 별도로 수행해야 한다.

## 4.9 운영 파라미터 확정사항 반영 검증

### 확정안

1. **AI Callback timeout = 180초**
   - AI 서버의 `202 Accepted`를 확인하고 Backend Job이 `PROCESSING`으로 전환된 시점부터 계산한다.
   - 180초 안에 `COMPLETED` 또는 `FAILED` Callback이 없으면 Backend Job을 `FAILED`로 변경하고 `AI_CALLBACK_TIMEOUT`을 기록한다.
   - Timeout 이후 늦은 `COMPLETED`는 기존 정책대로 `200 OK`로 멱등 처리하되 Backend Job은 `FAILED` 상태를 유지하고 Preview를 생성하지 않는다.

2. **AI → Backend Callback 재시도 = 최대 3회**
   - 최초 Callback 전송 실패 후 재시도 간격은 `2초 → 5초 → 10초`다.
   - 재시도 대상: Network error, HTTP `408`, `429`, `5xx`.
   - 재시도하지 않는 대상: HTTP `400`, `401`, `403`, `404`, `422` 및 그 밖의 재시도 대상으로 명시되지 않은 HTTP 응답.
   - `200 OK`를 받으면 전달 성공으로 보고 재시도를 종료한다.

3. **내부 서버 주소와 Internal API Key 환경별 분리**
   - Spring Boot → AI: `AI_SERVER_BASE_URL`
   - AI → Spring Boot Callback: `BACKEND_CALLBACK_BASE_URL`
   - 내부 인증 Key: `TRAVELBIRD_AI_CALLBACK_KEY`
   - 세 설정은 코드에 실제 값을 고정하지 않고 `local/dev/prod` 환경별 값으로 분리하여 관리한다.

### 1차 검증: 명시적 값 존재 여부

- Callback timeout `180초`: **확인**
- timeout 시작 기준 `Backend Job PROCESSING 전환 시점`: **확인**
- timeout 후 `FAILED + AI_CALLBACK_TIMEOUT`: **확인**
- timeout 후 늦은 COMPLETED `200 OK` 멱등 + FAILED 유지 + Preview 미생성: **확인**
- Callback 최대 재시도 `3회`: **확인**
- Backoff `2초 → 5초 → 10초`: **확인**
- 재시도 대상 Network error / `408` / `429` / `5xx`: **확인**
- `400` / `401` / `403` / `404` / `422` 비재시도: **확인**
- `AI_SERVER_BASE_URL`: **확인**
- `BACKEND_CALLBACK_BASE_URL`: **확인**
- `TRAVELBIRD_AI_CALLBACK_KEY` 환경별 분리: **확인**
- `local/dev/prod` 환경 분리: **확인**

### 2차 검증: 기존 정책과 교차 일관성

- 180초 timeout은 기존 `AI_CALLBACK_TIMEOUT` 상태 전이와 충돌하지 않는다.
- timeout 이후 늦은 `COMPLETED`의 `200 OK` 멱등 정책은 그대로 유지한다.
- Callback 재시도는 Backend의 중복 Callback 멱등 처리와 함께 동작하므로 동일 종료 결과가 재전송되어도 Preview 또는 실패 기록을 중복 생성하지 않는다.
- `401 INVALID_INTERNAL_AI_KEY`, `404 AI_JOB_NOT_FOUND`, `422 INVALID_AI_RESPONSE`가 비재시도 대상이므로 인증·작업 식별·계약 위반 오류를 불필요하게 반복 전송하지 않는다.
- Base URL 환경변수화는 내부 Endpoint Path와 DTO 계약을 변경하지 않는다.
- `TRAVELBIRD_AI_CALLBACK_KEY`의 환경별 분리는 기존 동일 Key 양방향 인증 정책과 충돌하지 않는다.

### 3차 검증: 구버전·미정 표현 잔존 여부

- Callback timeout 숫자 미정 활성 표현: **0건**
- Callback retry 횟수 미정 활성 표현: **0건**
- Callback backoff 간격 미정 활성 표현: **0건**
- 내부 Base URL을 코드에 하드코딩해야 한다는 활성 표현: **0건**
- 개발·운영에서 같은 Internal API Key를 강제로 공유한다는 활성 표현: **0건**

### 최종 판정

- 이번 운영 파라미터 확정사항: **3건**
- 기능명세서 반영: **3/3 완료**
- 운영 파라미터와 기존 v10 API·상태 전이 정책 간 신규 모순: **0건**
- 이번 변경으로 DTO 필드, 공개 API Endpoint, AI Callback Endpoint 자체를 변경한 항목: **0건**
- 실제 `AI_SERVER_BASE_URL`, `BACKEND_CALLBACK_BASE_URL`, `TRAVELBIRD_AI_CALLBACK_KEY`의 Secret/주소 값은 각 배포 환경에서 주입하며 문서에 실제 값을 기록하지 않는다.

## 4.10 내부 AI 요청 즉시 응답 규격 확정사항 반영 검증

### 확정안 반영

1. `POST /internal/v1/recommendations`의 즉시 응답은 `202 | 400 | 401 | 409 | 422`로 정의했다.
2. `202 Accepted`는 신규 정상 접수와 동일 `jobId`+동일 Payload 재전송의 멱등 접수에 사용한다.
3. 동일 `jobId`에 다른 Payload가 전달되면 `409 AI_JOB_ID_CONFLICT`를 반환한다.
4. `400 INVALID_REQUEST`는 필수 필드 누락, 타입·형식·Enum·Schema 제약 위반에 사용한다.
5. `401 INVALID_INTERNAL_AI_KEY`는 `X-Internal-AI-Key` 누락 또는 불일치에 사용한다.
6. `422 PLACE_NOT_FOUND`, `422 PLACE_REGION_MISMATCH`, `422 CONFLICTING_PLACE_POLICY`는 AI 서버의 방어적 도메인·정책 검증에 사용한다.
7. `400/409/422` 즉시 실패 Response는 모두 공통 `ErrorResponse{code, message, timestamp, details}`를 사용하며 별도 중첩 `ErrorBody`를 만들지 않는다.
8. 사용자 소유권, 저장 장소 관계, 위시리스트, 필수 배치 장소 수 `> dayCount × 15` 등 Backend가 판단할 수 있는 정책은 Spring Boot가 AI 호출 전에 우선 검증한다.

### 1단계 검증: API Status 및 Error Code 존재 여부

- `202 Accepted`: **확인**
- `400 INVALID_REQUEST`: **확인**
- `401 INVALID_INTERNAL_AI_KEY`: **확인**
- `409 AI_JOB_ID_CONFLICT`: **확인**
- `422 PLACE_NOT_FOUND`: **확인**
- `422 PLACE_REGION_MISMATCH`: **확인**
- `422 CONFLICTING_PLACE_POLICY`: **확인**

### 2단계 검증: 멱등성·오류 Schema·검증 책임

- 동일 `jobId`+동일 Payload → 새 작업 생성 없이 기존 접수 결과 `202`: **확인**
- 동일 `jobId`+다른 Payload → `409 AI_JOB_ID_CONFLICT`: **확인**
- `400/409/422` → 공통 `ErrorResponse`: **확인**
- 내부 즉시 오류에 `{error:{...}}` 중첩 ErrorBody 사용: **0건**
- AI `400/422`를 Backend 정책의 1차 검증으로 사용하는 활성 표현: **0건**
- `requiredPlaceCount > dayCount × 15`는 기존과 동일하게 Backend가 AI 호출 전에 검증: **확인**

### 3단계 검증: 기존 정책과 교차 일관성

- 새로운 `422 PLACE_NOT_FOUND | PLACE_REGION_MISMATCH | CONFLICTING_PLACE_POLICY`는 Backend→AI **요청 접수 단계**의 방어적 검증이고, AI Callback의 `422 INVALID_AI_RESPONSE`는 **AI 결과 검증 단계**이므로 역할이 겹치지 않는다.
- 공개 사용자 API의 `422 AI_ROUTE_REQUIRED_PLACE_LIMIT_EXCEEDED`는 Spring Boot가 AI 호출 전에 수행하는 **사용자 요청 사전 검증**이므로 내부 AI `422`와 충돌하지 않는다.
- Callback retry/backoff 정책의 `400/401/403/404/422 비재시도`는 AI→Backend Callback 전송 정책이고, 이번 `202/400/401/409/422`는 Backend→AI 요청 즉시 응답 정책이므로 방향과 역할이 다르다.
- Internal API Key, 180초 timeout, Callback 멱등 처리, AI 4상태/Backend 5상태, 기존 장소 KEEP, TRIP_WISHLIST MUST_INCLUDE 정책은 변경하지 않았다.

### 최종 판정

- 내부 AI 요청 즉시 응답 규격 확정안 반영: **8/8 완료**
- OpenAPI와 기능명세서 간 신규 모순: **0건**
- 기존 Callback 규격과의 충돌: **0건**
- 기존 공개 API의 사전 수용량 `422`와의 충돌: **0건**
- 이번 확정안에서 명시되지 않은 구현 세부사항은 임의로 새 Error Code나 DTO로 추가하지 않았다.



## 4.11 동일 Payload SHA-256 판정 규칙 확정사항 반영 검증

### 확정 규칙

- 동일 `jobId`의 동일 Payload 여부는 `jobId`를 제외한 `RecommendationJobRequest`의 정규화된 의미 Payload를 SHA-256 fingerprint로 비교하여 판단한다. JSON 객체 Key 순서, 공백, 개행은 무시한다. 순서가 의미 없는 `themes`, `savedPlaceIds`, `wishlistPlaceIds`는 정렬하여 비교하고, `existingSchedule`은 `dayNumber` 기준으로 정규화하되 각 Day의 `placeIds` 순서는 유지한다. 동일 fingerprint면 기존 접수 결과를 `202 Accepted`로 멱등 반환하고, 다르면 `409 AI_JOB_ID_CONFLICT`를 반환한다.

### 반영 검증

- `jobId` fingerprint 비교 대상 제외: **확인**
- `RecommendationJobRequest` 의미 Payload 기준: **확인**
- SHA-256 사용: **확인**
- JSON 객체 Key 순서 무시: **확인**
- 공백·개행 무시: **확인**
- `themes`, `savedPlaceIds`, `wishlistPlaceIds` 정렬: **확인**
- `existingSchedule` `dayNumber` 기준 정규화: **확인**
- 각 Day `placeIds` 순서 유지: **확인**
- 동일 fingerprint → `202 Accepted`: **확인**
- 다른 fingerprint → `409 AI_JOB_ID_CONFLICT`: **확인**
- fingerprint 전송 필드 추가 없음: **확인**
