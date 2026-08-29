# 08. Current Status

## A. Authoritative Sources

| Item | Status |
| --- | --- |
| `docs/source/backend-functional-spec-v10.md` | Confirmed source |
| `docs/source/travelbird-openapi-v10.json` | Confirmed source |
| `docs/source/ai-backend-integration-v10.md` | Confirmed source |
| `docs/source/frontend-api-ux-guide-v10.md` | Confirmed source |
| Source-of-truth priority | Defined in `AGENTS.md` and `docs/harness/00-source-of-truth.md` |

## B. Backend Harness

| Area | Status |
| --- | --- |
| `AGENTS.md` | Present |
| Domain contract harness | Present |
| Java 21 / Spring Boot 3.4 rule | Present |
| Lombok / MapStruct / QueryDSL rule | Present |
| Testcontainers / Flyway rule | Present |
| ErrorCode catalog and error templates | Present |
| Spring Security 6.4 CORS template | Present |
| `.env.example` | Present |
| `application.yml` secure template | Present |
| DBML | Present, pending implementation alignment from approved decisions |
| Custom project Skill | Not created |

## C. Database

DBML currently covers:

- User/Auth
- Onboarding
- File
- Region/Place
- Saved Place
- Trip
- Post/Community
- Saved Route
- Social
- Event
- AI Job/Preview

The user intentionally removed the local database helper directory. Do not recreate that helper file in this stage.

## D. Approved Design Decisions

The following items were approved by the user on 2026-08-29 and are the baseline for the next implementation stage.

1. Spring Boot patch: `3.4.13`
2. Gradle group/base package: `com.travelbird`
3. Manual AI Preview place `reason`: `사용자가 일정 미리보기에 직접 추가한 장소입니다.`
4. Event original TourAPI ID: internal `event_id` plus unique `tour_api_content_id`
5. FK `ON DELETE`: `CASCADE` for owned children, `RESTRICT` for aggregate/cross-domain references, `SET NULL` for nullable surviving references
6. UserStatus: `ACTIVE`, `SUSPENDED`, `WITHDRAWN`

## E. Next Development Stage

The approved first implementation plan is `docs/superpowers/plans/2026-08-29-backend-foundation.md`.

Recommended order:

1. Backend foundation: Gradle, Spring Boot entrypoint, DBML alignment, Flyway V1, Testcontainers verification.
2. Auth/User.
3. Onboarding.
4. Place/File.
5. Trip.
6. Post/Community.
7. SavedRoute/Social/Event/Home/Search.
8. AI Job/Preview.

