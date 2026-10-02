# 실제 Gemini AI–Backend E2E 결과와 Backend 수정 이유

## 결과

**실제 Gemini 추천 접수 → HTTP Callback → Backend SUCCEEDED → Preview 조회·수정·저장 → 새 Trip 생성/기존 Trip 반영까지 성공했습니다.** GENERAL, SAVED_PLACES, TRIP_WISHLIST 세 유형과 주요 예외를 확인했고 HTTP 응답·DB 상태 검사 **107개 모두 통과**했습니다.

이 결과는 2026-09-28~29 Windows 로컬에서 이번 PR에 포함된 Part1·Part2·Part3 수정 전체를 적용한 통합본으로 수행한 결과입니다. 이후 Part2만 분리하려던 작업은 취소하고 사용자 승인에 따라 통합 수정본으로 복원했습니다. 복원 뒤 전체 테스트를 다시 실행하며 결과는 상태 문서에 기록합니다. 실제 Gemini를 다시 호출한 새 E2E 결과로 표현하지 않습니다. main 병합·배포 성공을 의미하지 않습니다.

## 파트별 수정 이유

| 영역 | 수정 전 실제 문제 | 수정 내용 및 이유 |
|---|---|---|
| Part1 공통 보안 | Gemini 생성이 끝났는데 Backend가 내부 키를 보낸 Callback을 401로 거절하여 180초 후 AI_CALLBACK_TIMEOUT 발생 | 정확한 POST /internal/ai-callbacks/trip-recommendations만 사용자 JWT 요구에서 제외. 기존 InternalAiKeyAuthenticationFilter의 공통 키 검증은 유지. 서버 간 Callback에는 사용자 JWT가 없으므로 필요한 수정 |
| Part2 Preview 조회 | days와 days.places를 동시에 fetch하며 MultipleBagFetchException/500 발생 | days만 fetch하고 나머지는 서비스 트랜잭션에서 읽어 Preview GET/PATCH 정상화 |
| Part2 Trip 응답 | DTO에 남은 지연 로딩 컬렉션 때문에 트랜잭션 종료 후 JSON 직렬화 실패 | themes/hashtags를 일반 LinkedHashSet으로 복사해 생성·조회 응답 정상화 |
| Part2 Preview 만료 | 이미 EXPIRED인 작업을 다시 처리해 스케줄러 예외 발생 | SUCCEEDED만 만료 대상으로 선택하고 이미 EXPIRED이면 중복 처리를 건너뛰어 멱등성 확보 |
| Part2 표시 정보 제공 + Part3 저장 목록 | AI_PREVIEW 제목은 빈 값, 지역은 임시 값, 장소명·테마는 빈 배열 | AiPreviewDisplayReader로 최신 Preview 정보를 제공하고 SavedRouteListService에서 사용. 본인 소유의 유효한 PERMANENT Preview만 표시하며 불가 참조는 제외 |

Part3 목록은 기존 savedRouteId/sourceId/savedAt 및 cursor 의미를 유지합니다. updatedAt은 Preview 수정 시각을 사용하고 장소 순서는 Preview 순서를 따릅니다. required+nullable author/thumbnailUrl 키도 유지합니다. 기존 POST 유형 목록 정책은 바꾸지 않습니다.

이 변경은 서로 다른 파트의 연결 지점에서 발견된 Backend 결함을 함께 해결합니다. AI 코드·공개 API·기준 문서·DB 스키마·Enum·migration은 변경하지 않았습니다. 새로운 공개 API를 추가하지 않았고 표시 조회 인터페이스는 Backend 내부 Java 계약입니다.

## 실제 생성 결과

| 유형 | Job ID | Preview | 반영 결과 |
|---|---:|---:|---|
| GENERAL | 3530946653514442765 | 1 | 수정 후 새 Trip 1 생성 |
| SAVED_PLACES | 4658729262503487607 | 2 | sync 204 → 접수 202 → Callback → SUCCEEDED → 새 Trip 2 생성 |
| TRIP_WISHLIST | 6533580510077156532 | 3 | sync 204 → 접수 202 → Callback → SUCCEEDED → 기존 Trip 3 반영 |

AI의 COMPLETED만 확인하지 않고 Backend 검증·저장과 SUCCEEDED polling, Preview 사용, 최종 Trip 조회까지 확인했습니다. 기존 장소 KEEP, wishlist MUST_INCLUDE, 추가 추천 금지, 적용 전 기존 Trip 불변, 기존 TripPlace ID·메모 보존 및 반복 적용 멱등성을 확인했습니다.

## 주요 예외 검증

- 내부 키 누락/오류, JWT만 보낸 Callback, 내부 키만으로 공개 API 접근, 사용자 JWT 누락
- 다른 사용자의 Job/Preview 조회·적용, 임시 또는 소유권 불일치 Preview 저장 목록 제외
- stale version, explicit null, 중복/없는 장소, 하루 15곳 초과, 빈 saved/wishlist, 요청 한도
- AI 같은 jobId·payload 재접수, 다른 payload 충돌, 잘못된 schema/지역/장소
- 중복·잘못된 장소·FAILED Callback, 실제 180초 timeout 후 늦은 COMPLETED
- Preview 만료 및 반복 만료, 취소·게시 잠금 Trip 적용

예외 입력과 만료·소유권 변경·게시 잠금 상태 일부는 격리 DB에 의도적으로 구성한 테스트 조건입니다. Gemini가 실제로 잘못된 결과를 생성했다는 의미가 아닙니다.

## 환경과 한계

- Windows, Java 21.0.10, Spring Boot 3.4.13, MySQL 8.0.36, Python 3.11.5
- AI main b848056de02440017f8e5f1e76f1419bf22aac04, Backend 기준 d639d9568a0df3cb324c47b33d55a5f587ee2097 + 본 PR 변경
- 실제 gemini-3.1-flash-lite, thinking minimal, 후보 12/출력 2048, 재시도·재생성 0, rules fallback 비활성화
- canonical 장소 1,965개와 외부 매핑 1,966건 확인; provider/externalId/placeId/지역/카테고리 불일치 0건
- dataset SHA256: 0dd82b21db090b64ad20f60941aad7fb2ba033b100996781d05c35d71e1f8a78
- 원본 DB를 변경하지 않고 별도 테스트 DB·테스트 사용자 사용. 테스트 JWT는 로컬 생성
- 이전 통합본 clean test bootJar 367개 통과. 복원 후 재검증 결과는 08-current-status.md 참조
- Docker 부재로 Testcontainers 미실행. 실제 로컬 MySQL/Flyway로 검증했으며 H2로 대체하지 않음
- Python 3.12, 실제 Kakao 로그인, S3 업로드·사진 보존, 폭넓은 다일 품질·부하, 배포 환경은 미검증

AI Callback 최종 로그의 시도 횟수 표시 차이는 진단 로그 문제이며 E2E 성공을 막는 기능 장애가 아니었습니다. AI 수정은 이번 성공의 전제 조건이 아니고 AI 소스도 수정하지 않았습니다.

## 첨부 증거

[107개 검사 결과](ai-e2e-check-results.json)는 실제 결과 파일에서 검사 이름과 통과 여부만 추출한 자료입니다. 원시 응답, 인증 헤더, 실제 키, dataset은 포함하지 않았습니다. [AI 전달 메시지](ai-e2e-ai-message.md)와 [최종 테스트 상태](08-current-status.md)를 함께 참고해 주세요.

### AI 로그 표기 문제의 영향

확인된 AI Callback 로그 문제는 실제 1회 시도에서 종료됐을 때 최종 문구가 최대 횟수인 4회로 표시되는 문제입니다. **비즈니스 로직에는 문제가 없으며, 401에서 재시도하지 않는 동작도 계약과 일치합니다.** 이는 실제 재시도를 잘못 수행한 문제가 아닌 로그 표기 오류로, 서비스 처리 및 이번 E2E 성공에 영향이 없습니다. AI 필수 수정 요청이나 통합 차단 사유로 취급하지 않습니다.
