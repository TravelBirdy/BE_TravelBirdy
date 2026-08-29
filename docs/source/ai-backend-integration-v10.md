# TravelBird

## AI 추천 서버 연동 및 공통 계약 가이드


> **문서 목적**
>
> AI 추천 서버 1차 구현을 최종 Backend 기능명세서와 OpenAPI 계약에 맞추기 위한 전달 문서입니다. 질문 1~7의 답변뿐 아니라 내부 Request/즉시 응답/Callback, requestType별 정책, 검증 책임, 운영 파라미터, 통합 테스트 체크리스트까지 포함합니다.


### 기준 문서

`backend-functional-spec-v10.md`

`travelbird-openapi-v10.json`

※ 실제 Internal API Key 및 실서버 URL 값은 문서에 포함하지 않습니다.

> **Repository Source 안내**
>
> Backend Repository에서는 이 Markdown 파일을 AI 연동 기준 문서로 사용한다.
> 작업 기준은 이 Markdown 파일이다.
> 필드 타입·required/nullable·Enum·HTTP Status의 기계 판독 기준은 `travelbird-openapi-v10.json`,
> 비즈니스 정책은 `backend-functional-spec-v10.md`를 우선한다.


## 0. 먼저 공유할 결론


> **연동 방향은 맞습니다**
>
> Spring Boot가 jobId를 생성해 AI에 비동기 작업을 접수하고, AI는 202 + PENDING으로 즉시 접수한 뒤 COMPLETED 또는 FAILED Callback을 Spring Boot에 전달합니다. 다만 기존 AI 초안의 callback URL, 기존 일정 처리, pace 해석, 오류/시간 필드 등은 아래 최종 계약으로 맞춰야 합니다.


```text
Frontend
  -> Spring Boot public AI API
  -> Backend Job 생성 (QUEUED, jobId: Long)
  -> PUT {AI_SERVER_BASE_URL}/internal/v1/places/sync (필요한 런타임 장소가 있을 때)
  -> POST {AI_SERVER_BASE_URL}/internal/v1/recommendations
  <- 202 Accepted + PENDING + 동일 jobId + acceptedAt
  -> Backend PROCESSING (여기서 180초 timeout 시작)
  -> AI 추천 수행
  -> POST {BACKEND_CALLBACK_BASE_URL}/internal/ai-callbacks/trip-recommendations
     status = COMPLETED 또는 FAILED
  -> Spring Boot 검증 / Preview 저장 / Backend Job 최종 상태 반영
  -> Frontend는 Spring Boot job 상태 API를 polling
```


### AI 파트에 그대로 전달할 답변문

AI 추천 서버 1차 구현 방향은 맞습니다. 최종 연동은 Spring Boot -> POST /internal/v1/recommendations -> AI 202 + PENDING + 동일 jobId -> AI 추천 수행 -> POST /internal/ai-callbacks/trip-recommendations로 COMPLETED/FAILED를 전달하는 구조입니다.

다만 최종 기획/API 기준으로 아래 항목은 반드시 맞춰 주세요. placeId는 Backend 내부 places PK이며 Java Long / OpenAPI int64 / JSON number입니다. 관광 API contentId 등 외부 식별자는 places에 직접 저장하지 않고 place_external_ids에서 별도 관리합니다. places.placeId는 서비스 내부 canonical 장소 ID이며, place_external_ids는 placeId/provider/externalPlaceId를 관리하고 (provider, externalPlaceId)를 Unique로 둡니다. MVP provider는 KTO_TOUR_API와 NAVER를 사용합니다. regionCode는 별도 지역 Enum이 아니라 법정동 코드 앞 5자리 시군구 String 하나입니다. jobId는 Spring Boot가 Long으로 생성하고 AI가 동일 값을 즉시 응답과 Callback에 그대로 사용합니다. Callback URL은 /internal/v1/recommendation-jobs/{jobId}/result가 아니라 POST /internal/ai-callbacks/trip-recommendations입니다. 양방향 내부 호출 모두 X-Internal-AI-Key를 사용합니다.

existingSchedule은 기존 장소를 제외하고 남은 슬롯만 채우는 방식이 아닙니다. 기존 장소는 전부 KEEP하되 Day/order는 재배치할 수 있고, AI는 기존 장소 + 필수 위시리스트 + 허용된 추가 장소를 포함한 최종 전체 일정을 반환해야 합니다. pace는 RELAXED/NORMAL/DENSE이며 2/3/4곳 고정 개수 의미가 아닙니다. recommendedTime, durationMinutes, plannedTime은 null로 보내는 것이 아니라 MVP Schema에서 제거합니다.

Backend->AI 즉시 응답은 202/400/401/409/422로 확정됐습니다. 같은 jobId+같은 Payload 재전송은 새 작업 없이 기존 접수 결과를 202로 멱등 반환하고, 같은 jobId+다른 Payload는 409 AI_JOB_ID_CONFLICT입니다. 400/409/422는 공통 ErrorResponse를 사용합니다. AI의 400/422는 방어적 검증이고, 소유권/저장 장소 관계/위시리스트/requiredPlaceCount > dayCount*15 등 Backend가 아는 정책은 Spring Boot가 먼저 검증합니다.

Callback timeout은 Backend Job이 PROCESSING으로 바뀐 시점부터 180초입니다. AI->Backend Callback 실패 시 최초 전송 이후 최대 3회 재시도하며 2초, 5초, 10초 간격을 사용합니다. Network error, 408, 429, 5xx만 재시도하고 그 외 응답은 재시도하지 않습니다. Base URL과 Internal API Key는 local/dev/prod 환경별로 분리합니다.

reason은 모든 AI 반환 장소에서 필수이며, 공백을 제외한 실제 내용 기준 최소 10자, 최대 100자입니다. 공백으로 길이를 채우지 말고 장소·동선·테마·필수 포함 사유 등을 설명하는 의미 있는 문장을 생성해 주세요. 이 규칙은 기능명세와 최신 OpenAPI의 AiResultPlace.reason / AiPreviewPlace.reason에 동일하게 반영되어 있습니다.


## 1. 질문 1~7 최종 답변


| 질문 | 판정 | 최종 답변 |
| --- | --- | --- |
| 1. placeId | 확정 | Backend 내부 places PK(canonical placeId). Java Long / OpenAPI integer(int64) / JSON number. TourAPI/NAVER 외부 ID는 place_external_ids에서 provider + externalPlaceId로 별도 관리하며 (provider, externalPlaceId)는 Unique. 하나의 placeId에 여러 Provider 외부 ID를 연결할 수 있음. AI는 존재하지 않는 placeId를 만들지 않음. |
| 2. regionCode | 확정 | 법정동 코드 앞 5자리 시군구 코드 String 1개. SEOUL/BUSAN 같은 자체 Enum 사용 안 함. 여행 생성/AI 추천은 단일 시군구. |
| 3. jobId | 확정 | Spring Boot가 Long으로 생성. AI는 동일 jobId를 202 응답과 Callback에 그대로 사용. 문자열 jobId 새로 생성 금지. |
| 4. Callback API | 수정 필요 | POST /internal/ai-callbacks/trip-recommendations. COMPLETED/FAILED 동일 Endpoint, status로 구분. X-Internal-AI-Key 필수. |
| 5. existingSchedule | 수정 필요 | 기존 장소 수만큼 신규 추천 슬롯을 단순 차감하지 않음. 기존 장소는 모두 KEEP, Day/order 재배치 가능. AI가 최종 전체 schedule을 반환. |
| 6. Error 규격 | 확정 | 즉시 응답 400/401/409/422 역할 분리. 400/409/422는 공통 ErrorResponse. FAILED Callback은 jobId/status + code/message/timestamp/details의 flat 구조. |
| 7. 시간/체류시간 | 제거 | recommendedTime, durationMinutes, plannedTime은 null placeholder가 아니라 Schema 자체에서 제거. MVP에서 생성/관리하지 않음. |


> **중요**
>
> NORMAL = 하루 최대 3곳, 기존 2곳이면 신규 1곳 같은 구현은 최종 정책과 충돌합니다. Day별 시스템 하드캡은 15곳이지만 pace의 의미와는 별개입니다.


## 2. 식별자·지역·기본 타입 공통 규칙


| 항목 | 최종 계약 | AI 구현 시 주의 |
| --- | --- | --- |
| placeId | Long/int64, JSON number | AI·데이터 정규화 데이터에서도 Backend 내부 canonical placeId를 사용. TourAPI contentId/NAVER 외부 ID를 placeId로 보내지 않음. TourAPI는 KTO_TOUR_API + contentId, NAVER는 NAVER + externalPlaceId로 Backend place_external_ids에 연결. |
| jobId | Long/int64, JSON number | Spring Boot 생성. 응답/Callback에서 그대로 echo. 문자열 변환 금지. |
| regionCode | 5자리 시군구 String | 한 요청에 정확히 한 지역. 새 추천 장소는 해당 regionCode 범위 안에서 선택. |
| startDate/endDate | ISO date | travelDays를 authoritative 필드로 사용하지 않음. 필요 시 두 날짜로 계산. |
| companionType | SOLO/COUPLE/FRIENDS/FAMILY/OTHER | 단일 값. |
| themes | 6종 중 1~3개, 중복 금지 | ACTIVITY/SNS_HOTPLACE/NATURE/ATTRACTION/SHOPPING/FOOD. |
| pace | RELAXED/NORMAL/DENSE | 고정 장소 수가 아니라 일정 밀도·이동 강도. |

- AI·데이터 파트가 사전 수집·정규화한 관광지 데이터에서 후보 장소를 선정합니다. 추천 시 관광 API를 실시간 호출하는 구조가 아닙니다. 초기 TourAPI 데이터의 contentId는 외부 ID로 유지하고 Backend 초기 import 후 전달받은 placeId를 추천 데이터에 반영합니다.
- 외부 검색 결과를 내부 저장 기능에서 사용하려면 Backend의 place resolve 흐름을 통해 canonical placeId가 먼저 확보됩니다. /api/places/resolve는 NAVER 검색 결과 전용 공개 흐름이며 provider는 Backend 내부에서 NAVER로 처리합니다.

### 2.1 places + place_external_ids 및 관광데이터 적재 확정

- 서비스 내부 실제 장소는 places.placeId 하나를 canonical ID로 사용하고, 외부 Provider 식별자는 place_external_ids에서 분리 관리합니다.
- place_external_ids는 placeId, provider, externalPlaceId를 관리하고 (provider, externalPlaceId)에 Unique 제약을 둡니다. MVP provider는 KTO_TOUR_API | NAVER입니다.
- TourAPI contentId는 provider=KTO_TOUR_API, externalPlaceId=contentId로 관리하며 Backend placeId로 직접 사용하지 않습니다.
- AI/관광데이터 파트가 TourAPI 데이터를 사전 수집·정규화해 CSV/JSON으로 Backend에 전달하면 Backend가 Seeder/import script 또는 일회성 batch로 places와 place_external_ids를 적재합니다. 공개 Bulk API나 /api/places/resolve 대량 반복 호출은 사용하지 않습니다.
- 초기 import 후 Backend는 최소 provider / externalPlaceId / placeId 매핑을 AI/관광데이터 파트에 반환하고, AI 추천 데이터는 이 placeId를 반영합니다.
- AI/데이터 파트는 장소명·주소·좌표 정규화와 동일 장소 후보 추출을 지원합니다. 최종 병합 여부, placeId 생성, 외부 ID 연결은 Backend가 결정합니다.
- 자동 병합은 정규화 장소명 일치 AND 정규화 주소 일치 AND 좌표 거리 50m 이내이며 고신뢰 후보가 정확히 1개일 때만 허용합니다. 후보가 없거나 복수·불명확하면 잘못된 병합을 피하기 위해 새 placeId를 생성합니다.
- 운영 중 NAVER-only 신규 장소가 AI 추천의 savedPlaceIds/wishlistPlaceIds/existingSchedule에 참조될 수 있으므로 Backend는 추천 작업 전달 전에 필요한 canonical 장소를 PUT /internal/v1/places/sync로 AI에 먼저 동기화합니다.
- PUT /internal/v1/places/sync는 placeId, regionCode, name, address, latitude, longitude, category를 places[]로 전달하고 AI는 placeId 기준 멱등 upsert합니다. 성공은 204이며, 400 INVALID_REQUEST / 401 INVALID_INTERNAL_AI_KEY는 공통 ErrorResponse를 사용합니다.
- 장소 sync 실패 시 Backend job을 FAILED(AI_PLACE_SYNC_FAILED)로 처리하고 /internal/v1/recommendations를 호출하지 않습니다. RecommendationJobRequest와 Callback DTO의 placeId 구조는 변경하지 않습니다.

## 3. Backend -> AI RecommendationJobRequest


| 필드 | 타입/제약 | 의미 |
| --- | --- | --- |
| jobId | Long / required | Spring Boot 발급 작업 ID |
| requestType | GENERAL \| SAVED_PLACES \| TRIP_WISHLIST / required | 추천 정책 구분 |
| regionCode | String / required / ^[0-9]{5}$ | 단일 시군구 |
| startDate | date / required | 여행 시작일 |
| endDate | date / required | 여행 종료일 |
| companionType | CompanionType / required | 단일 동행 유형 |
| themes | TravelTheme[] / required / 1~3 / unique | 여행 테마 |
| pace | RELAXED \| NORMAL \| DENSE / required | 정성적 밀도/이동 강도 |
| savedPlaceIds | Long[] / required / non-null | 해당 없으면 [] |
| wishlistPlaceIds | Long[] / required / non-null | 해당 없으면 [] |
| existingSchedule | ExistingScheduleDay[] / required / non-null | 해당 없으면 [] |
| allowAdditionalRecommendations | Boolean / required | 추가 보완 추천 허용 여부 |


```json
{
  "jobId": 123,
  "requestType": "TRIP_WISHLIST",
  "regionCode": "11110",
  "startDate": "2026-08-20",
  "endDate": "2026-08-22",
  "companionType": "FRIENDS",
  "themes": ["FOOD", "SNS_HOTPLACE"],
  "pace": "NORMAL",
  "savedPlaceIds": [],
  "wishlistPlaceIds": [301, 302],
  "existingSchedule": [
    {"dayNumber": 1, "placeIds": [101, 102]},
    {"dayNumber": 2, "placeIds": [201]}
  ],
  "allowAdditionalRecommendations": true
}
```


## 4. requestType별 정책


| 구분 | GENERAL | SAVED_PLACES | TRIP_WISHLIST |
| --- | --- | --- | --- |
| 목적 | 일반 조건 추천 | 저장 장소 선호 반영 | 기존 Trip + 위시리스트 전체 재배치 |
| savedPlaceIds | [] | 선택값, 선호 신호 | [] |
| wishlistPlaceIds | [] | [] | 모두 MUST_INCLUDE |
| existingSchedule | [] | [] | Backend가 DB에서 생성, 모두 KEEP |
| allowAdditionalRecommendations | true | true | 사용자 입력 true/false |
| 기존 Day/order 이동 | 해당 없음 | 해당 없음 | 가능 |
| AI 반환 | 최종 전체 일정 | 최종 전체 일정 | 기존+위시리스트+허용 추가를 포함한 최종 전체 일정 |


> **SAVED_PLACES와 TRIP_WISHLIST를 섞지 말 것**
>
> savedPlaceIds는 반드시 포함해야 하는 목록이 아닙니다. 반면 TRIP_WISHLIST의 wishlistPlaceIds는 전부 포함해야 하고 existingSchedule의 기존 장소도 모두 유지해야 합니다.


## 5. POST /internal/v1/recommendations 즉시 응답


| HTTP | code / body | 사용 조건 |
| --- | --- | --- |
| 202 | AiServerAcceptedResponse | 신규 정상 접수 또는 동일 jobId + 동일 Payload 재전송. PENDING + 동일 jobId + acceptedAt. |
| 400 | INVALID_REQUEST | 필수 필드 누락, 타입/형식/Enum/Schema 제약 위반. |
| 401 | INVALID_INTERNAL_AI_KEY | X-Internal-AI-Key 누락 또는 불일치. |
| 409 | AI_JOB_ID_CONFLICT | 같은 jobId가 이미 있으나 기존 요청과 다른 Payload. |
| 422 | PLACE_NOT_FOUND | Backend의 추천 전 장소 동기화 이후에도 내부 placeId를 AI 추천 데이터에서 확인할 수 없는 방어적 오류. |
| 422 | PLACE_REGION_MISMATCH | 장소가 요청 regionCode와 일치하지 않음. |
| 422 | CONFLICTING_PLACE_POLICY | requestType/기존 일정/저장 장소/위시리스트/추가 추천 정책 간 의미 충돌. |


```json
// 202
{
  "jobId": 123,
  "status": "PENDING",
  "acceptedAt": "2026-08-13T22:30:00+09:00"
}

// 400 / 409 / 422 공통 ErrorResponse 예
{
  "code": "INVALID_REQUEST",
  "message": "Recommendation request validation failed.",
  "timestamp": "2026-08-13T22:30:00+09:00",
  "details": {"field": "regionCode"}
}
```

- 동일 jobId의 동일 Payload 여부는 jobId를 제외한 RecommendationJobRequest의 정규화된 의미 Payload를 SHA-256 fingerprint로 비교하여 판단합니다. JSON 객체 Key 순서, 공백, 개행은 무시합니다. 순서가 의미 없는 themes, savedPlaceIds, wishlistPlaceIds는 정렬하여 비교하고, existingSchedule은 dayNumber 기준으로 정규화하되 각 Day의 placeIds 순서는 유지합니다. 동일 fingerprint면 기존 접수 결과를 202 Accepted로 멱등 반환하고, 다르면 409 AI_JOB_ID_CONFLICT를 반환합니다.

## 6. existingSchedule과 최종 일정 반환 규칙

1. existingSchedule의 모든 장소는 최종 결과에서 제거하면 안 됩니다. 기존 장소는 ALWAYS KEEP입니다.
1. existingSchedule의 dayNumber와 placeIds 배열 순서는 현재 상태를 알려주는 참고값이며, AI가 다른 Day로 이동하거나 순서를 변경할 수 있습니다.
1. TRIP_WISHLIST의 wishlistPlaceIds는 전부 MUST_INCLUDE입니다.
1. allowAdditionalRecommendations=false는 새 보완 장소만 막습니다. 기존 장소/필수 위시리스트를 삭제하는 옵션이 아닙니다.
1. AI는 신규 추천 장소만 Callback하지 않고, 기존 + 필수 위시리스트 + 허용된 신규를 포함한 최종 전체 days를 Callback합니다.
1. Spring Boot는 old schedule과 AI 신규 장소를 다시 merge하지 않습니다. 전체 AI 결과를 검증해 Preview로 저장합니다.
1. 하나의 최종 일정 전체에서 동일 placeId는 정확히 최대 1회만 존재할 수 있습니다. 서로 다른 Day 반복도 금지입니다.
1. Day별 시스템 하드캡은 15곳입니다. pace와 별개의 플랫폼 제한입니다.
1. 모든 반환 장소는 reason이 필수입니다. 기존 장소도 결과에 포함되므로 기존 장소에도 reason을 반환해야 합니다.

## 7. AI -> Backend Callback 계약

최종 Endpoint


```text
POST {BACKEND_CALLBACK_BASE_URL}/internal/ai-callbacks/trip-recommendations
X-Internal-AI-Key: {environment secret}
```

COMPLETED Callback


```json
{
  "jobId": 123,
  "status": "COMPLETED",
  "tripTitle": "서울 종로 감성 여행",
  "summary": "먹거리와 문화 공간을 균형 있게 구성한 일정입니다.",
  "days": [
    {
      "dayNumber": 1,
      "places": [
        {"placeId": 101, "order": 1, "reason": "동선 시작점으로 접근성이 좋고 테마와 잘 맞습니다."},
        {"placeId": 301, "order": 2, "reason": "필수 위시리스트 장소이며 인접 장소와 연결이 자연스럽습니다."}
      ]
    }
  ],
  "requestId": "optional-request-id",
  "schemaVersion": "optional-version"
}
```

- required: jobId, status, tripTitle, summary, days
- 각 장소 required: placeId(Long), order(Int >=1), reason(String, 공백을 제외한 실제 내용 기준 10~100자)
- requestId와 schemaVersion은 optional이며 누락 가능
- recommendedTime, durationMinutes, plannedTime은 보내지 않음
FAILED Callback


```json
{
  "jobId": 123,
  "status": "FAILED",
  "code": "PLACE_NOT_FOUND",
  "message": "추천에 필요한 장소 데이터를 찾을 수 없습니다.",
  "timestamp": "2026-08-13T22:31:00+09:00",
  "details": {"placeIds": [501]}
}
```

- FAILED는 {error:{...}} 중첩 구조가 아니라 flat DTO입니다.
- details만 optional+nullable이며, code/message/timestamp는 필수입니다.
- FAILED Callback의 code는 처리 중 실패 원인을 전달하는 String입니다. 즉시 응답용 Error Code와 동일 코드를 사용할 수 있지만 exhaustive enum으로 제한된 것은 아닙니다.

## 8. Callback 수신 결과와 Backend 검증


| Callback 결과 | Backend HTTP | Backend 처리 |
| --- | --- | --- |
| COMPLETED + 전체 검증/저장 성공 | 200 | Preview 생성, Backend Job SUCCEEDED, previewId 반환 |
| FAILED 정상 수신 | 200 | Backend Job FAILED, Preview 없음 |
| 같은 종료 결과 중복 Callback | 200 | 멱등 처리, 중복 Preview/실패기록 생성 안 함 |
| AI_CALLBACK_TIMEOUT 후 늦은 COMPLETED | 200 | 기존 FAILED 유지, Preview 생성 안 함 |
| Internal Key 오류 | 401 | `INVALID_INTERNAL_AI_KEY`, Body 처리 안 함 |
| 없는 jobId | 404 | `AI_JOB_NOT_FOUND`, 결과 저장 안 함 |
| COMPLETED 구조/계약 검증 실패 | 422 | `INVALID_AI_RESPONSE`, Preview 생성 안 함, Backend Job FAILED |

Backend가 COMPLETED 전체 결과에서 검증하는 핵심

- 모든 placeId가 Backend 내부 DB에 존재하는지
- Day 번호가 여행 기간 안에 있는지
- Day별 order 누락/중복이 없는지
- Day별 장소 수가 15개 이하인지
- 전체 days에서 동일 placeId가 중복되지 않는지(다른 Day 중복도 금지)
- reason이 계약을 만족하는지
- TRIP_WISHLIST라면 existingSchedule의 모든 기존 장소와 wishlistPlaceIds가 모두 포함됐는지
- allowAdditionalRecommendations=false이면 필수 외 신규 장소가 추가되지 않았는지

> **AI COMPLETED ≠ Backend SUCCEEDED**
>
> AI가 COMPLETED를 보냈더라도 Spring Boot의 전체 결과 검증과 Preview 저장까지 성공해야 Backend Job이 SUCCEEDED가 됩니다.


## 9. AI 일정 생성 공통 규칙

- pace는 고정 개수 정책이 아닙니다. RELAXED는 여유/짧은 이동, NORMAL은 균형, DENSE는 더 높은 밀도이되 비현실적 왕복을 피합니다.
- 하루 2/3/4곳 같은 고정 수를 사용하지 않습니다.
- existingSchedule 장소는 모두 유지하지만 Day/order 재배치는 가능합니다.
- TRIP_WISHLIST wishlistPlaceIds는 전부 포함합니다.
- SAVED_PLACES savedPlaceIds는 선호 신호이며 반드시 포함할 필요가 없습니다.
- allowAdditionalRecommendations=true이면 보완 장소 추가 가능, false이면 보완 장소만 금지합니다.
- 신규 장소만 반환하지 않고 최종 전체 일정을 반환합니다.
- 새 추천 장소는 regionCode 범위 안에서 선택합니다.
- 존재하지 않는 placeId를 만들지 않습니다.
- 시간/체류시간 필드는 생성하지 않습니다.
- 최종 전체 일정에서 동일 placeId를 두 번 이상 배치하지 않습니다.
- Day별 최대 15곳을 준수합니다.
- 모든 반환 장소에 reason을 생성합니다.

> **reason 계약 정합성 보완 완료**
>
> 기능명세의 "공백 제외 최소 10자, 최대 100자" 규칙을 OpenAPI의 AiResultPlace.reason과 AiPreviewPlace.reason description에도 명시했습니다. AI는 공백을 길이 충족 수단으로 사용하지 않고 실제 의미 있는 추천 이유를 생성합니다.


## 10. 운영 파라미터 및 배포 공통 규칙


| 항목 | 확정값 | AI 서버 구현 포인트 |
| --- | --- | --- |
| Callback timeout | 180초 | AI 202 수신 후 Backend가 PROCESSING으로 전환한 시점부터. 늦은 COMPLETED는 성공 복구되지 않음. |
| Callback 재시도 | 최대 3회 재시도 | 최초 전송 + 최대 3회 재시도 = 최대 4번 전송 시도. |
| Backoff | 2초 -> 5초 -> 10초 | 각 재시도 전 대기. |
| 재시도 대상 | Network error / 408 / 429 / 5xx | 일시 장애만 재시도. |
| 비재시도 | 400 / 401 / 403 / 404 / 409 / 422 및 그 밖의 미지정 HTTP | 재시도하지 않음. |
| Backend->AI URL | AI_SERVER_BASE_URL | 코드 하드코딩 금지. |
| AI->Backend URL | BACKEND_CALLBACK_BASE_URL | 코드 하드코딩 금지. |
| Internal Key | TRAVELBIRD_AI_CALLBACK_KEY | local/dev/prod 별도 Secret. Header X-Internal-AI-Key. |
| Transport | HTTPS | 실 Secret을 소스/Git/일반 로그에 남기지 않음. |


> **Timeout 실무 의미**
>
> 180초가 지나 Backend가 AI_CALLBACK_TIMEOUT으로 FAILED가 된 뒤 AI가 COMPLETED를 보내도 Callback은 200으로 수신되지만 결과는 버려지고 Preview가 생성되지 않습니다. AI는 이 시간 안에 최종 Callback을 보내도록 목표를 잡아야 합니다.


## 11. 상태 모델과 멱등성


| AI 서버 상태 | Backend 상태 | 설명 |
| --- | --- | --- |
| PENDING | PROCESSING | AI가 202로 접수한 뒤 Backend가 PROCESSING으로 전환 |
| RUNNING | PROCESSING | AI 내부 실행 상태. 중간 Callback 불필요 |
| COMPLETED | SUCCEEDED 조건부 | Callback 수신 + Backend 검증 + Preview 저장 성공 후에만 SUCCEEDED |
| FAILED | FAILED | FAILED Callback 정상 반영 |
| - | QUEUED | Backend가 job 생성했으나 AI 접수 확인 전 |
| - | EXPIRED | 연결된 TEMPORARY Preview가 만료된 상태 |

- Backend->AI: same jobId + same Payload => 202 멱등, 새 작업 생성 금지.
- Backend->AI: same jobId + different SHA-256 fingerprint => 409 AI_JOB_ID_CONFLICT.
- AI->Backend: 같은 jobId의 같은 종료 결과 Callback 재전송 => Backend 200 멱등.
- MVP에서는 작업 취소, SSE, WebSocket, push 완료 알림을 사용하지 않습니다.

## 12. Backend와 AI의 책임 경계


| Spring Boot가 AI 호출 전에 책임지는 것 | AI 서버가 책임지는 것 |
| --- | --- |
| 사용자 인증/소유권 | RecommendationJobRequest Schema 방어 검증 |
| savedPlaceIds가 실제 사용자 저장 장소인지 | AI 정규화 데이터에서 placeId 존재 확인 |
| Trip wishlist/existingSchedule 조회 및 구성 | placeId와 regionCode 의미 검증 |
| TRIP_WISHLIST 필수 장소 수용량: distinct(required) > dayCount×15 차단 | requestType/배열/allowAdditionalRecommendations 의미 충돌 방어 검증 |
| 발행 Post 경로 잠금, 취소 Trip 등 변경 가능성 | 정규화 데이터 후보 선정 및 경로 구성 |
| COMPLETED 결과 전체 검증과 Preview 저장 | 최종 전체 일정 + title + summary + 모든 reason 생성 |

- AI 내부 즉시 오류 Body를 Frontend에 직접 노출하지 않습니다. Frontend는 Spring Boot 공개 API와 Job status만 봅니다.
- Backend가 전달하는 existingSchedule에는 tripPlaceId가 없습니다. 전체 Trip에서 placeId 중복이 금지되어 있으므로 AI 재배치 식별은 placeId로 충분하며, 메모/사진 보존은 Backend가 적용 단계에서 기존 TripPlace를 재사용해 처리합니다.

### 장소 기준데이터 책임 경계

- AI/관광데이터: TourAPI 사전 수집·정규화, 초기 동일 장소 후보 추출 지원, Backend가 반환한 placeId를 추천용 데이터에 반영합니다.
- Backend: places/place_external_ids 저장, 최종 동일 장소 병합 판단, placeId 발급 및 external ID 연결을 담당합니다.
- 운영 중 NAVER-only 장소가 추천에 참조되면 Backend가 PUT /internal/v1/places/sync로 추천 전에 canonical 장소 정보를 동기화합니다. AI는 placeId 기준으로 멱등 upsert합니다.
- Frontend는 기존 externalPlaceId -> /api/places/resolve -> placeId 흐름만 사용하며 place_external_ids나 내부 sync API를 직접 호출하지 않습니다.

## 13. 기존 AI 초안에서 반드시 제거/변경할 항목


| 기존 초안/가정 | 최종 기준 |
| --- | --- |
| callback URL /internal/v1/recommendation-jobs/{jobId}/result | /internal/ai-callbacks/trip-recommendations |
| jobId string | Long/int64 JSON number |
| region Enum 예: SEOUL | 5자리 시군구 String |
| travelDays authoritative | startDate/endDate 사용 |
| scheduleDensity | pace |
| RELAXED/NORMAL/FULL | RELAXED/NORMAL/DENSE |
| NORMAL=3곳 등 2/3/4 고정 | 삭제 |
| themeTags string[] | TravelTheme 6종 themes[] |
| existingPlaceMode KEEP/REPLACE | 삭제, existingSchedule는 항상 KEEP |
| 기존 장소를 결과에서 제외 | 삭제, 전체 최종 일정 반환 |
| SAVED_PLACES must include | 삭제, 선호 참고 |
| TRIP_WISHLIST 가중치만 | MUST_INCLUDE |
| recommendedTime / durationMinutes / plannedTime = null | 필드 자체 제거 |
| 중첩 AI ErrorBody | 즉시 오류는 공통 ErrorResponse, FAILED Callback은 flat |
| Internal 인증 미정 | 양방향 X-Internal-AI-Key |


## 14. 통합 테스트 체크리스트

- 01. 정상 GENERAL 요청 -> AI 202 PENDING + 동일 jobId + acceptedAt
- 02. 정상 SAVED_PLACES -> savedPlaceIds를 선호 신호로 사용하고 신규 장소 추천 가능
- 03. 정상 TRIP_WISHLIST -> existingSchedule 전부 + wishlist 전부 포함한 전체 일정 Callback
- 04. 같은 jobId + 정규화 후 동일 SHA-256 fingerprint 재전송 -> 202, AI 작업 중복 생성 없음
- 05. 같은 jobId + 다른 SHA-256 fingerprint -> 409 AI_JOB_ID_CONFLICT
- 06. Internal Key 누락/오류 -> 401 INVALID_INTERNAL_AI_KEY
- 07. 필수 필드/Enum/형식 오류 -> 400 INVALID_REQUEST
- 08. AI 데이터에 placeId 없음 -> 422 PLACE_NOT_FOUND
- 09. placeId 지역 불일치 -> 422 PLACE_REGION_MISMATCH
- 10. requestType 정책 의미 충돌 -> 422 CONFLICTING_PLACE_POLICY
- 11. COMPLETED 정상 -> Backend 200 + previewId 생성
- 12. FAILED 정상 -> Backend 200 + previewId null + Job FAILED
- 13. COMPLETED에 존재하지 않는 placeId -> Backend 422 INVALID_AI_RESPONSE
- 14. COMPLETED 전체 days에 동일 placeId 중복 -> Backend 422, Preview 없음
- 15. Day 15곳 초과 -> Backend 422, Preview 없음
- 16. reason 누락/길이 오류 -> Backend 결과 검증 실패
- 17. TRIP_WISHLIST 기존 장소 누락 -> Backend 결과 검증 실패
- 18. TRIP_WISHLIST wishlist 누락 -> Backend 결과 검증 실패
- 19. allowAdditionalRecommendations=false인데 추가 신규 장소 존재 -> Backend 결과 검증 실패
- 20. 같은 종료 Callback 재전송 -> Backend 200 멱등, 중복 Preview 없음
- 21. Callback network error -> 2초/5초/10초 최대 3회 재시도
- 22. Callback 408/429/5xx -> 재시도
- 23. Callback 400/401/403/404/409/422 및 그 밖의 미지정 HTTP -> 재시도하지 않음
- 24. Callback 200 -> 즉시 재시도 종료
- 25. 180초 Callback 없음 -> Backend FAILED + AI_CALLBACK_TIMEOUT
- 26. Timeout 뒤 늦은 COMPLETED -> Backend 200, FAILED 유지, Preview 미생성
- 27. recommendedTime/durationMinutes/plannedTime이 Request/Callback에 존재하지 않음
- 28. AI COMPLETED 상태와 Backend SUCCEEDED 상태가 분리되어 있음
- 29. TourAPI 초기 데이터 import -> Backend places/place_external_ids 생성 + KTO_TOUR_API/contentId 매핑 + placeId 발급
- 30. 동일 (provider, externalPlaceId) 재적재 -> 기존 canonical placeId 재사용, 중복 external mapping 없음
- 31. KTO/NAVER 동일 장소가 정규화 이름·주소 일치 + 좌표 50m 이내 + 단일 고신뢰 후보 -> 하나의 placeId에 외부 ID 복수 연결
- 32. 동일 장소 후보가 없거나 복수/불명확 -> 자동 병합하지 않고 새 placeId 생성
- 33. NAVER-only 장소가 savedPlaceIds/wishlistPlaceIds/existingSchedule에 포함 -> PUT /internal/v1/places/sync 204 후 추천 접수
- 34. place sync 실패 -> Backend Job FAILED + AI_PLACE_SYNC_FAILED, /internal/v1/recommendations 미호출
- 35. place sync 후 Recommendation Request/Callback -> 외부 ID 없이 기존 placeId Long 계약 유지

## 부록. AI 파트가 우선 확인할 OpenAPI 항목

- PUT /internal/v1/places/sync
- AiPlaceSyncRequest / AiPlaceSyncItem
- POST /internal/v1/recommendations
- RecommendationJobRequest / AiServerAcceptedResponse / ErrorResponse
- POST /internal/ai-callbacks/trip-recommendations
- AiRecommendationCompletedCallbackRequest / AiRecommendationFailedCallbackRequest / AiCallbackResponse
- ExistingScheduleDay / AiResultDay / AiResultPlace
- AiRequestType / Pace / TravelTheme / CompanionType / BackendAiJobStatus
- 이번 places + place_external_ids 분리 및 장소 sync 확정은 RecommendationJobRequest와 AI Callback의 기존 placeId 필드 구조를 변경하지 않습니다.
이 문서는 AI 연동 요약·계약 가이드이며, 필드 타입·required/nullable·Enum·HTTP Response의 기계 판독 기준은 `travelbird-openapi-v10.json`을 사용한다. 기능 해석과 운영 정책은 `backend-functional-spec-v10.md`와 함께 확인한다.
