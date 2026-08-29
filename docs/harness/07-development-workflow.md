# 07. Development Workflow

## 기존 v10 기능 구현

1. OpenAPI에서 Operation과 Schema 확인
2. 기능명세에서 Business Rule 확인
3. `01-domain-contract.md` 확인
4. DB 변경 여부 판단
5. DB 변경 시 DBML 수정 -> Flyway migration 작성
6. Request/Response record 작성
7. Entity/Repository/QueryDSL 작성
8. MapStruct mapper 작성
9. Service business logic + ownership
10. Controller
11. ErrorCode/Global handler 연결
12. Unit test
13. Testcontainers MySQL integration test
14. OpenAPI와 Response 재검증
15. `08-current-status.md` 갱신

## 신규 정책/계약 변경

기존 4개 기준 파일로 답이 나지 않는 경우:

1. `Superpowers: Brainstorming`
2. 선택지와 영향 범위 작성
3. 사용자 결정
4. Source of Truth 변경
5. DBML/OpenAPI/Flyway/코드 반영
6. 테스트

임의로 "일반적인 방식"을 선택해 계약을 바꾸지 않는다.

## DB 변경 체크리스트

- PK
- FK
- NOT NULL
- Unique
- Cursor용 Index
- 검색/필터 Index
- 동시성 충돌
- soft/tombstone 정책
- Flyway rollback이 아닌 forward migration
- MySQL 8.0 Testcontainers 검증

## Definition of Done

- `./gradlew test` 성공
- Flyway migration startup 성공
- MySQL Testcontainers 성공
- OpenAPI required/nullable 일치
- ErrorCode 일치
- Entity 직접 노출 없음
- Service ownership 검증 존재
- Secret 하드코딩 없음
- DBML/Flyway 동기화
