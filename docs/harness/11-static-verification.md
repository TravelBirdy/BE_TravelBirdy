# Harness v2 Static Verification

- ✅ DBML table count: 38
- ✅ DBML tables unique: True
- ✅ place external unique exists: True
- ✅ trip global place unique exists: True
- ✅ preview global place unique exists: True
- ✅ report unique exists: True
- ✅ saved route unique exists: True
- ✅ Security CORS lambda: True
- ✅ OPTIONS preflight: True
- ✅ credentials true: True
- ✅ maxAge set: True
- ✅ env yml no literal DB password: True
- ✅ env yml no literal JWT secret: True
- ✅ env yml no literal NAVER secret: True
- ✅ Flyway enabled: True
- ✅ Error codes extracted: 98

- DBML tables: users, user_social_accounts, refresh_tokens, personality_tests, personality_questions, personality_options, personality_submissions, personality_answers, files, sigungu_master, places, place_external_ids, place_category_mapping_rules, saved_places, home_recommended_places, trips, trip_themes, trip_hashtags, trip_days, trip_places, trip_place_images, trip_wishlist_places, posts, post_images, post_places, post_hashtags, post_view_histories, post_daily_metrics, reports, saved_routes, follows, user_blocks, events, ai_recommendation_jobs, ai_trip_previews, ai_preview_hashtags, ai_preview_days, ai_preview_places

정적 검증은 파일 구조/핵심 문자열/제약 존재 여부 검증이다.
실제 Flyway migration 실행 및 Spring Boot compile/test는 Backend 프로젝트 dependency 초기화 후 수행한다.

- ✅ UserStatus 전체 literal 미확정 상태를 DB enum으로 임의 고정하지 않음
