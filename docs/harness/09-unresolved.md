# 09. Unresolved / Explicitly Not Invented

As of 2026-08-29, the previous six backend bootstrap questions have been approved by the user and no longer block the next implementation plan.

## Approved On 2026-08-29

1. Spring Boot patch: `3.4.13`
2. Gradle group/base package: `com.travelbird`
3. Manual AI Preview place `reason`: `사용자가 일정 미리보기에 직접 추가한 장소입니다.`
4. Event original TourAPI ID: internal `event_id` plus unique `tour_api_content_id`
5. FK `ON DELETE`: `CASCADE` for owned children, `RESTRICT` for aggregate/cross-domain references, `SET NULL` for nullable surviving references
6. UserStatus: `ACTIVE`, `SUSPENDED`, `WITHDRAWN`

## Current Unresolved Items

No additional unresolved backend bootstrap decisions are recorded in this file.
