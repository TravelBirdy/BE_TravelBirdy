# AI HTTP E2E 통합 수정 범위

- Task: 사용자 승인에 따라 Part1/Part2/Part3 Backend 수정 전체를 한 PR로 공유.
- Allowlist: SecurityConfig, SavedRouteListService 및 테스트; AI Preview 조회/만료/표시 인터페이스·구현; TripMapper; 관련 회귀 테스트와 E2E 결과 문서.
- Source: v10 내부 Callback 인증, Preview 사용·만료·저장 목록 및 Trip 응답 계약.
- Contract: 공개 HTTP API, DB 스키마, migration 변경 없음. AI Python 소스 변경 없음.
- Evidence: 동일 통합 수정본에서 실제 Gemini E2E 107개 검사 성공. 전체 테스트를 복원 후 재실행.
- Secret: 실제 키, .env, canonical dataset, 원시 응답/로그를 커밋하지 않음. 검증 항목·결과만 공유.
