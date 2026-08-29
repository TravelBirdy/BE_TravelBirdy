# 10. Recommended Final Harness Structure

```text
TravelBird-BE/
├─ AGENTS.md
├─ .env.example
│
├─ docs/
│  ├─ database/
│  │  └─ travelbird.dbml
│  └─ harness/
│     ├─ 00-source-of-truth.md
│     ├─ 01-domain-contract.md
│     ├─ 02-backend-rules.md
│     ├─ 03-error-contract.md
│     ├─ 04-database-design.md
│     ├─ 05-security-cors.md
│     ├─ 06-local-environment.md
│     ├─ 07-development-workflow.md
│     ├─ 08-current-status.md
│     ├─ 09-unresolved.md
│     └─ 10-final-structure.md
│
└─ src/main/
   ├─ resources/
   │  ├─ application.yml
   │  └─ db/migration/
   │     └─ (Flyway migration — 개발 시작 시 생성)
   │
   └─ java/com/travelbird/global/
      ├─ config/
      │  ├─ CorsProperties.java
      │  └─ SecurityConfig.java
      └─ error/
         ├─ ErrorCode.java
         ├─ AiJobFailureCode.java
         ├─ ErrorResponse.java
         ├─ BusinessException.java
         └─ GlobalExceptionHandler.java
```

## 만들지 않은 것

- `.agents/skills/*`
- 기능별 중복 PRD
- API별 중복 명세
- 별도 FE 개발 규칙
- 별도 AI 서버 개발 규칙
- Reward/character growth schema
- Weather cache schema
- route polyline schema
- 관리자 Web schema

이 구조는 Backend 구현에 필요한 Source/Rule/Schema/Config만 남긴 최소 Harness다.
