# 04. Database Design

DBML: `docs/database/travelbird.dbml`

## 설계 범위

이번 DBML은 4개 기준 파일에서 영속 상태가 필요한 Backend 기능을 구현하기 위한 관계형 구조다.

포함:
- User/Auth
- Onboarding
- File
- Region/Place
- Saved Place
- Trip/Day/Place/Wishlist
- Post/Image/Place/Hashtag
- Community metric
- Report
- Saved Route
- Follow/Block
- Home fixed recommended place
- Event
- AI Job/Preview

명시적으로 제외:
- Reward/character growth
- 관리자 전용 Web/API용 별도 테이블
- route polyline/거리/예상 이동시간 저장
- weather cache
- Redis session/blacklist
- AI 시간/체류시간
- 임의의 장소 태그 확장

## 핵심 DB 제약

1. `place_external_ids(provider, external_place_id)` PK/Unique
2. `saved_places(user_id, place_id)` PK
3. `trip_wishlist_places(trip_id, place_id)` PK
4. `trip_places(trip_id, place_id)` Unique — Trip 전체 장소 중복 금지
5. `trip_places(trip_day_id, visit_order)` Unique
6. `ai_preview_places(preview_id, place_id)` Unique — Preview 전체 장소 중복 금지
7. `ai_preview_places(preview_day_id, visit_order)` Unique
8. `reports(reporter_user_id, post_id)` Unique
9. `saved_routes(user_id, source_type, source_id)` Unique
10. Follow/Block pair PK
11. 사용자별 Refresh Token row 1개
12. 사용자별 성향 테스트 제출 1개
13. 삭제되지 않은 Post per Trip 1개 — `active_trip_id` generated column + unique

## Post active_trip_id

Flyway에서 MySQL generated column으로 구현한다.

```sql
active_trip_id BIGINT
GENERATED ALWAYS AS (
    CASE WHEN deleted_at IS NULL THEN trip_id ELSE NULL END
) STORED
```

그리고 Unique Index:

```sql
CREATE UNIQUE INDEX uk_posts_active_trip
ON posts(active_trip_id);
```

MySQL Unique Index는 NULL을 여러 개 허용하므로 삭제된 Post tombstone 여러 개를 유지하면서 활성 Post만 1개로 제한할 수 있다.

## Trip/Preview 전체 placeId Unique

같은 placeId가 다른 Day에 반복되는 것도 금지되므로 Day 단위 Unique만으로는 부족하다.

- Trip: `UNIQUE(trip_id, place_id)`
- Preview: `UNIQUE(preview_id, place_id)`

Day와 parent의 일관성을 위해 `trip_places`/`ai_preview_places`에 parent ID를 함께 둔다.

## NOT NULL 정책

NOT NULL:
- API/도메인상 반드시 존재하는 ID, Enum, 날짜, 좌표, owner, order
- Array/관계가 별도 테이블인 경우 relation key

Nullable:
- Kakao email
- nickname/introduction
- birdType(온보딩 전)
- Trip summary
- cancelledAt
- SavedPlace memo
- TripPlace memo/reason(MANUAL)
- Post DRAFT title/content
- representative file
- publishedAt/deletedAt
- AI Job processing/error timestamps
- AI Preview expiresAt/savedAt/applied fields
- 만료 tombstone의 preview title/summary

## 서비스 계층에서 함께 검증할 제약

DB만으로 충분하지 않은 규칙:

- Theme 1~3개
- Day 장소 <= 15
- Post image <= 10
- TripPlace image <= 3
- Post hashtag <= 5
- Trip Day와 Wishlist 동일 place 동시 존재 금지
- 자기 자신 Follow/Block 금지
- Block 시 Follow 삭제
- canonical auto merge 4조건
- home recommended place 정확히 7개
- SavedRoute polymorphic source 유효성
- Post representativeFileId가 해당 Post 이미지 집합에 포함되는지
