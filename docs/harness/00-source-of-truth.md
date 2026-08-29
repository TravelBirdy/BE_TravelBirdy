# 00. Source of Truth

## 기준 파일 4개

| 우선 | 파일 | Backend에서 기준으로 삼는 범위 |
|---|---|---|
| 1 | `travelbird-openapi-v10.json` | Method, Path, Schema, required/nullable, Enum, HTTP Status |
| 2 | `backend-functional-spec-v10.md` | Business rule, 권한, 상태 전이, DB 요구, MVP 범위 |
| 3 | `ai-backend-integration-v10.md` | Backend의 AI Job/Callback/동기화 책임 |
| 4 | `frontend-api-ux-guide-v10.md` | 공개 API 응답 상태·required/nullable·PATCH·Cursor 의미 |

## 이번 Harness v2에서 추가로 확정한 Backend 기술 결정

사용자 요청으로 다음을 Backend 개발 표준으로 확정한다.

- Java 21 LTS
- Spring Boot 3.4.x
- Spring Security 6.4.x
- Lombok
- MapStruct
- QueryDSL
- Testcontainers
- Flyway
- immutable DTO는 Java record 적극 사용
- MySQL 8.0+
- `.env`는 로컬 Secret 주입용이며 Git에 커밋하지 않음
- `application.yml`은 `${ENV_KEY}` 참조만 사용
- CORS는 SecurityFilterChain + CorsConfigurationSource 방식
- 외부 장소 ID 매핑의 다른 canonical place 충돌은 `PLACE_EXTERNAL_ID_MAPPING_CONFLICT`

## 변경 원칙

- 기존 v10 기능을 구현할 때 새 PRD/API를 다시 설계하지 않는다.
- 계약을 바꾸어야 할 때는 먼저 기준 문서의 변경이 필요하다.
- DBML은 기능명세의 영속 상태와 API를 구현하기 위한 관계형 설계다.
- 명세에서 구현 선택지로 남긴 것은 `09-unresolved.md`에 별도로 기록하며, 임의 확정하지 않는다.
