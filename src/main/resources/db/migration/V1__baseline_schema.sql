-- =========================================================
-- V1__baseline_schema.sql
-- TravelBird 최종 ERD(docs/database/travelbird.dbml) 기준 초기 스키마
-- 담당: Part 1(eunjin) — 39개 테이블 전체 (Part1 13 + Part2 12 + Part3 14)
-- 규칙:
--   - Enum 컬럼은 VARCHAR로 통일 (애플리케이션 Enum이 허용값 검증)
--   - FK는 전체 테이블 생성 뒤 하단에서 일괄 ALTER TABLE로 추가
--   - dbml에 길이가 명시되지 않은 varchar는 용도에 맞춰 합리적으로 지정함
-- =========================================================

-- =========================================================
-- USER / AUTH
-- =========================================================

CREATE TABLE users (
    user_id               BIGINT       NOT NULL AUTO_INCREMENT,
    email                 VARCHAR(255) NULL,
    nickname              VARCHAR(10)  NULL,
    introduction          VARCHAR(100) NULL,
    status                VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    role                  VARCHAR(30)  NOT NULL,
    onboarding_completed  BOOLEAN      NOT NULL DEFAULT FALSE,
    bird_type             VARCHAR(30)  NULL,
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_social_accounts (
    user_id           BIGINT       NOT NULL,
    provider          VARCHAR(30)  NOT NULL,
    provider_user_id  VARCHAR(255) NOT NULL,
    PRIMARY KEY (provider, provider_user_id),
    UNIQUE KEY uk_user_social_user_provider (user_id, provider)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE refresh_tokens (
    user_id     BIGINT       NOT NULL,
    token_hash  VARCHAR(255) NOT NULL,
    expires_at  DATETIME     NOT NULL,
    revoked_at  DATETIME     NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- ONBOARDING
-- =========================================================

CREATE TABLE personality_tests (
    test_version  VARCHAR(20) NOT NULL,
    active        BOOLEAN     NOT NULL,
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (test_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE personality_questions (
    question_id     BIGINT      NOT NULL AUTO_INCREMENT,
    test_version    VARCHAR(20) NOT NULL,
    question_order  INT         NOT NULL,
    text            TEXT        NOT NULL,
    PRIMARY KEY (question_id),
    UNIQUE KEY uk_personality_question_order (test_version, question_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE personality_options (
    option_id    BIGINT      NOT NULL AUTO_INCREMENT,
    question_id  BIGINT      NOT NULL,
    text         TEXT        NOT NULL,
    trait        VARCHAR(30) NOT NULL,
    PRIMARY KEY (option_id),
    UNIQUE KEY uk_personality_option_question (question_id, option_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE personality_submissions (
    submission_id   BIGINT      NOT NULL AUTO_INCREMENT,
    user_id         BIGINT      NOT NULL,
    test_version    VARCHAR(20) NOT NULL,
    status          VARCHAR(30) NOT NULL,
    selected_trait  VARCHAR(30) NULL,
    tied_traits     JSON        NULL,
    bird_type       VARCHAR(30) NULL,
    gourmet_score   INT         NOT NULL,
    rest_score      INT         NOT NULL,
    photo_score     INT         NOT NULL,
    activity_score  INT         NOT NULL,
    culture_score   INT         NOT NULL,
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at    DATETIME    NULL,
    PRIMARY KEY (submission_id),
    UNIQUE KEY uk_personality_submission_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE personality_answers (
    submission_id  BIGINT NOT NULL,
    question_id    BIGINT NOT NULL,
    option_id      BIGINT NOT NULL,
    PRIMARY KEY (submission_id, question_id),
    KEY idx_personality_answer_question_option (question_id, option_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- FILE
-- =========================================================

CREATE TABLE files (
    file_id       BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL,
    file_name     VARCHAR(255) NOT NULL,
    object_key    VARCHAR(255) NOT NULL,
    content_type  VARCHAR(100) NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    width         INT          NOT NULL,
    height        INT          NOT NULL,
    purpose       VARCHAR(30)  NOT NULL,
    status        VARCHAR(30)  NOT NULL,
    expires_at    DATETIME     NOT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (file_id),
    UNIQUE KEY uk_files_object_key (object_key),
    KEY idx_files_cleanup_owner (user_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- REGION / PLACE
-- =========================================================

CREATE TABLE sigungu_master (
    sigungu_code  VARCHAR(5)   NOT NULL,
    sigungu_name  VARCHAR(100) NOT NULL,
    PRIMARY KEY (sigungu_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE places (
    place_id           BIGINT        NOT NULL AUTO_INCREMENT,
    status             VARCHAR(30)   NOT NULL DEFAULT 'ACTIVE',
    name               VARCHAR(255)  NOT NULL,
    description        TEXT          NULL,
    external_category  VARCHAR(255)  NULL,
    category           VARCHAR(30)   NOT NULL,
    address            VARCHAR(255)  NOT NULL,
    sigungu_code       VARCHAR(5)    NOT NULL,
    latitude           DECIMAL(10,7) NOT NULL,
    longitude          DECIMAL(11,7) NOT NULL,
    image_url          VARCHAR(500)  NULL,
    created_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (place_id),
    KEY idx_places_sigungu (sigungu_code),
    KEY idx_places_region_category_status (sigungu_code, category, status),
    KEY idx_places_name_address (name, address)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE place_external_ids (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    place_id            BIGINT       NOT NULL,
    provider            VARCHAR(30)  NOT NULL,
    external_place_id   VARCHAR(255) NOT NULL,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_provider_external_id (provider, external_place_id),
    KEY idx_place_external_place (place_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE place_category_mapping_rules (
    rule_id          BIGINT       NOT NULL AUTO_INCREMENT,
    priority         INT          NOT NULL,
    include_keyword  VARCHAR(255) NOT NULL,
    target_category  VARCHAR(30)  NOT NULL,
    excluded         BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (rule_id),
    UNIQUE KEY uk_place_category_mapping_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE saved_places (
    user_id     BIGINT       NOT NULL,
    place_id    BIGINT       NOT NULL,
    memo        VARCHAR(100) NULL,
    saved_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, place_id),
    KEY idx_saved_places_cursor (user_id, saved_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE home_recommended_places (
    place_id       BIGINT   NOT NULL,
    display_order  INT      NOT NULL,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (place_id),
    UNIQUE KEY uk_home_recommended_places_order (display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- TRIP
-- =========================================================

CREATE TABLE trips (
    trip_id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    source_type     VARCHAR(30)  NOT NULL,
    title           VARCHAR(255) NOT NULL,
    summary         TEXT         NULL,
    sigungu_code    VARCHAR(5)   NOT NULL,
    start_date      DATE         NOT NULL,
    end_date        DATE         NOT NULL,
    companion_type  VARCHAR(30)  NOT NULL,
    pace            VARCHAR(30)  NOT NULL,
    visibility      VARCHAR(30)  NOT NULL DEFAULT 'PRIVATE',
    cancelled_at    DATETIME     NULL,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (trip_id),
    KEY idx_trips_user_start (user_id, start_date, trip_id),
    KEY idx_trips_user_end (user_id, end_date, trip_id),
    KEY idx_trips_sigungu (sigungu_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE trip_themes (
    trip_id  BIGINT      NOT NULL,
    theme    VARCHAR(30) NOT NULL,
    PRIMARY KEY (trip_id, theme)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE trip_hashtags (
    trip_id  BIGINT      NOT NULL,
    hashtag  VARCHAR(10) NOT NULL,
    PRIMARY KEY (trip_id, hashtag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE trip_days (
    trip_day_id  BIGINT NOT NULL AUTO_INCREMENT,
    trip_id      BIGINT NOT NULL,
    day_number   INT    NOT NULL,
    PRIMARY KEY (trip_day_id),
    UNIQUE KEY uk_trip_day_number (trip_id, day_number),
    UNIQUE KEY uk_trip_day_trip_pair (trip_id, trip_day_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE trip_places (
    trip_place_id  BIGINT       NOT NULL AUTO_INCREMENT,
    trip_id        BIGINT       NOT NULL,
    trip_day_id    BIGINT       NOT NULL,
    place_id       BIGINT       NOT NULL,
    visit_order    INT          NOT NULL,
    memo           VARCHAR(100) NULL,
    reason         VARCHAR(100) NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (trip_place_id),
    UNIQUE KEY uk_trip_place_global (trip_id, place_id),
    UNIQUE KEY uk_trip_day_visit_order (trip_day_id, visit_order),
    KEY idx_trip_place_trip_day (trip_id, trip_day_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE trip_place_images (
    trip_place_id  BIGINT NOT NULL,
    file_id        BIGINT NOT NULL,
    display_order  INT    NOT NULL,
    PRIMARY KEY (trip_place_id, file_id),
    UNIQUE KEY uk_trip_place_image_order (trip_place_id, display_order),
    UNIQUE KEY uk_trip_place_image_file (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE trip_wishlist_places (
    trip_id   BIGINT   NOT NULL,
    place_id  BIGINT   NOT NULL,
    added_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (trip_id, place_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- POST / COMMUNITY
-- =========================================================

CREATE TABLE posts (
    post_id                 BIGINT       NOT NULL AUTO_INCREMENT,
    trip_id                 BIGINT       NOT NULL,
    title                   VARCHAR(50)  NULL,
    content                 TEXT         NULL,
    representative_file_id  BIGINT       NULL,
    status                  VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    visibility              VARCHAR(30)  NOT NULL DEFAULT 'PRIVATE',
    view_count              BIGINT       NOT NULL DEFAULT 0,
    save_count              BIGINT       NOT NULL DEFAULT 0,
    share_count             BIGINT       NOT NULL DEFAULT 0,
    published_at            DATETIME     NULL,
    deleted_at              DATETIME     NULL,
    active_trip_id          BIGINT       GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN trip_id ELSE NULL END) STORED,
    version                 BIGINT       NOT NULL DEFAULT 0,
    created_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (post_id),
    UNIQUE KEY uk_posts_active_trip (active_trip_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE post_images (
    post_id        BIGINT NOT NULL,
    file_id        BIGINT NOT NULL,
    display_order  INT    NOT NULL,
    PRIMARY KEY (post_id, file_id),
    UNIQUE KEY uk_post_image_order (post_id, display_order),
    UNIQUE KEY uk_post_image_file (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE post_places (
    post_id        BIGINT NOT NULL,
    trip_place_id  BIGINT NOT NULL,
    PRIMARY KEY (post_id, trip_place_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE post_hashtags (
    post_id  BIGINT      NOT NULL,
    hashtag  VARCHAR(10) NOT NULL,
    PRIMARY KEY (post_id, hashtag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE post_view_histories (
    view_history_id  BIGINT   NOT NULL AUTO_INCREMENT,
    post_id          BIGINT   NOT NULL,
    viewer_user_id   BIGINT   NOT NULL,
    viewed_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (view_history_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE post_daily_metrics (
    post_id      BIGINT NOT NULL,
    metric_date  DATE   NOT NULL,
    view_count   BIGINT NOT NULL DEFAULT 0,
    save_count   BIGINT NOT NULL DEFAULT 0,
    share_count  BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (post_id, metric_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE post_shares (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    post_id     BIGINT      NOT NULL,
    user_id     BIGINT      NULL,
    channel     VARCHAR(30) NOT NULL,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_post_shares_post (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE reports (
    report_id         BIGINT      NOT NULL AUTO_INCREMENT,
    reporter_user_id  BIGINT      NOT NULL,
    post_id           BIGINT      NOT NULL,
    reason_code       VARCHAR(30) NOT NULL,
    description       TEXT        NULL,
    status             VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
    created_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (report_id),
    UNIQUE KEY uk_report_user_post (reporter_user_id, post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- SAVED ROUTE / SOCIAL
-- =========================================================

CREATE TABLE saved_routes (
    saved_route_id    BIGINT      NOT NULL AUTO_INCREMENT,
    user_id           BIGINT      NOT NULL,
    source_type       VARCHAR(30) NOT NULL,
    source_id         BIGINT      NOT NULL,
    source_available  BOOLEAN     NOT NULL DEFAULT TRUE,
    saved_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (saved_route_id),
    UNIQUE KEY uk_saved_route_source (user_id, source_type, source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE follows (
    follower_user_id   BIGINT   NOT NULL,
    following_user_id  BIGINT   NOT NULL,
    followed_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (follower_user_id, following_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_blocks (
    blocker_user_id  BIGINT   NOT NULL,
    blocked_user_id  BIGINT   NOT NULL,
    blocked_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (blocker_user_id, blocked_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- EVENT
-- =========================================================

CREATE TABLE events (
    event_id             BIGINT       NOT NULL AUTO_INCREMENT,
    tour_api_content_id  VARCHAR(50)  NOT NULL,
    name                 VARCHAR(255) NOT NULL,
    sigungu_code         VARCHAR(5)   NOT NULL,
    place_name           VARCHAR(255) NOT NULL,
    start_date           DATE         NOT NULL,
    end_date             DATE         NOT NULL,
    thumbnail_url        VARCHAR(500) NULL,
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (event_id),
    UNIQUE KEY uk_events_tour_api_content_id (tour_api_content_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- AI JOB / PREVIEW
-- =========================================================

CREATE TABLE ai_recommendation_jobs (
    job_id                             BIGINT       NOT NULL,
    user_id                            BIGINT       NOT NULL,
    target_trip_id                     BIGINT       NULL,
    request_type                       VARCHAR(30)  NOT NULL,
    status                             VARCHAR(30)  NOT NULL,
    region_code                        VARCHAR(5)   NOT NULL,
    start_date                         DATE         NOT NULL,
    end_date                           DATE         NOT NULL,
    companion_type                     VARCHAR(30)  NOT NULL,
    pace                               VARCHAR(30)  NOT NULL,
    themes                             JSON         NOT NULL,
    saved_place_ids                    JSON         NOT NULL,
    wishlist_place_ids                 JSON         NOT NULL,
    existing_schedule                  JSON         NOT NULL,
    allow_additional_recommendations   BOOLEAN      NOT NULL,
    requested_at                       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at                         DATETIME     NULL,
    completed_at                       DATETIME     NULL,
    expired_at                         DATETIME     NULL,
    error_code                         VARCHAR(100) NULL,
    error_message                      VARCHAR(500) NULL,
    error_retryable                    BOOLEAN      NULL,
    PRIMARY KEY (job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_trip_previews (
    preview_id        BIGINT       NOT NULL AUTO_INCREMENT,
    job_id            BIGINT       NOT NULL,
    user_id           BIGINT       NOT NULL,
    trip_title        VARCHAR(255) NULL,
    summary           TEXT         NULL,
    retention_status  VARCHAR(30)  NOT NULL,
    expires_at        DATETIME     NULL,
    saved_at          DATETIME     NULL,
    applied_trip_id   BIGINT       NULL,
    applied_at        DATETIME     NULL,
    version           BIGINT       NOT NULL DEFAULT 0,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (preview_id),
    UNIQUE KEY uk_ai_trip_previews_job (job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_preview_hashtags (
    preview_id  BIGINT      NOT NULL,
    hashtag     VARCHAR(10) NOT NULL,
    PRIMARY KEY (preview_id, hashtag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_preview_days (
    preview_day_id  BIGINT NOT NULL AUTO_INCREMENT,
    preview_id      BIGINT NOT NULL,
    day_number      INT    NOT NULL,
    PRIMARY KEY (preview_day_id),
    UNIQUE KEY uk_ai_preview_day (preview_id, day_number),
    UNIQUE KEY uk_ai_preview_day_preview_pair (preview_id, preview_day_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_preview_places (
    preview_place_id  BIGINT       NOT NULL AUTO_INCREMENT,
    preview_id        BIGINT       NOT NULL,
    preview_day_id    BIGINT       NOT NULL,
    place_id          BIGINT       NOT NULL,
    visit_order       INT          NOT NULL,
    reason            VARCHAR(100) NOT NULL,
    PRIMARY KEY (preview_place_id),
    UNIQUE KEY uk_ai_preview_place_global (preview_id, place_id),
    UNIQUE KEY uk_ai_preview_day_order (preview_day_id, visit_order),
    KEY idx_ai_preview_place_preview_day (preview_id, preview_day_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- REFERENCES / DELETE POLICY
-- 전체 테이블 생성 후 일괄 추가 (생성 순서 의존성 회피)
-- =========================================================

ALTER TABLE user_social_accounts
    ADD CONSTRAINT fk_user_social_accounts_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE;

ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE;

ALTER TABLE personality_questions
    ADD CONSTRAINT fk_personality_questions_test_version FOREIGN KEY (test_version) REFERENCES personality_tests (test_version) ON DELETE CASCADE;

ALTER TABLE personality_options
    ADD CONSTRAINT fk_personality_options_question FOREIGN KEY (question_id) REFERENCES personality_questions (question_id) ON DELETE CASCADE;

ALTER TABLE personality_submissions
    ADD CONSTRAINT fk_personality_submissions_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_personality_submissions_test_version FOREIGN KEY (test_version) REFERENCES personality_tests (test_version) ON DELETE RESTRICT;

ALTER TABLE personality_answers
    ADD CONSTRAINT fk_personality_answers_submission FOREIGN KEY (submission_id) REFERENCES personality_submissions (submission_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_personality_answers_question FOREIGN KEY (question_id) REFERENCES personality_questions (question_id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_personality_answers_option FOREIGN KEY (question_id, option_id) REFERENCES personality_options (question_id, option_id) ON DELETE RESTRICT;

ALTER TABLE files
    ADD CONSTRAINT fk_files_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE;

ALTER TABLE places
    ADD CONSTRAINT fk_places_sigungu FOREIGN KEY (sigungu_code) REFERENCES sigungu_master (sigungu_code) ON DELETE RESTRICT;

ALTER TABLE place_external_ids
    ADD CONSTRAINT fk_place_external_ids_place FOREIGN KEY (place_id) REFERENCES places (place_id) ON DELETE CASCADE;

ALTER TABLE saved_places
    ADD CONSTRAINT fk_saved_places_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_saved_places_place FOREIGN KEY (place_id) REFERENCES places (place_id) ON DELETE CASCADE;

ALTER TABLE home_recommended_places
    ADD CONSTRAINT fk_home_recommended_places_place FOREIGN KEY (place_id) REFERENCES places (place_id) ON DELETE CASCADE;

ALTER TABLE trips
    ADD CONSTRAINT fk_trips_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_trips_sigungu FOREIGN KEY (sigungu_code) REFERENCES sigungu_master (sigungu_code) ON DELETE RESTRICT;

ALTER TABLE trip_themes
    ADD CONSTRAINT fk_trip_themes_trip FOREIGN KEY (trip_id) REFERENCES trips (trip_id) ON DELETE CASCADE;

ALTER TABLE trip_hashtags
    ADD CONSTRAINT fk_trip_hashtags_trip FOREIGN KEY (trip_id) REFERENCES trips (trip_id) ON DELETE CASCADE;

ALTER TABLE trip_days
    ADD CONSTRAINT fk_trip_days_trip FOREIGN KEY (trip_id) REFERENCES trips (trip_id) ON DELETE CASCADE;

ALTER TABLE trip_places
    ADD CONSTRAINT fk_trip_places_trip FOREIGN KEY (trip_id) REFERENCES trips (trip_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_trip_places_trip_day FOREIGN KEY (trip_id, trip_day_id) REFERENCES trip_days (trip_id, trip_day_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_trip_places_place FOREIGN KEY (place_id) REFERENCES places (place_id) ON DELETE RESTRICT;

ALTER TABLE trip_place_images
    ADD CONSTRAINT fk_trip_place_images_trip_place FOREIGN KEY (trip_place_id) REFERENCES trip_places (trip_place_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_trip_place_images_file FOREIGN KEY (file_id) REFERENCES files (file_id) ON DELETE RESTRICT;

ALTER TABLE trip_wishlist_places
    ADD CONSTRAINT fk_trip_wishlist_places_trip FOREIGN KEY (trip_id) REFERENCES trips (trip_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_trip_wishlist_places_place FOREIGN KEY (place_id) REFERENCES places (place_id) ON DELETE CASCADE;

ALTER TABLE posts
    ADD CONSTRAINT fk_posts_trip FOREIGN KEY (trip_id) REFERENCES trips (trip_id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_posts_representative_file FOREIGN KEY (representative_file_id) REFERENCES files (file_id) ON DELETE SET NULL;

ALTER TABLE post_images
    ADD CONSTRAINT fk_post_images_post FOREIGN KEY (post_id) REFERENCES posts (post_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_post_images_file FOREIGN KEY (file_id) REFERENCES files (file_id) ON DELETE RESTRICT;

ALTER TABLE post_places
    ADD CONSTRAINT fk_post_places_post FOREIGN KEY (post_id) REFERENCES posts (post_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_post_places_trip_place FOREIGN KEY (trip_place_id) REFERENCES trip_places (trip_place_id) ON DELETE RESTRICT;

ALTER TABLE post_hashtags
    ADD CONSTRAINT fk_post_hashtags_post FOREIGN KEY (post_id) REFERENCES posts (post_id) ON DELETE CASCADE;

ALTER TABLE post_view_histories
    ADD CONSTRAINT fk_post_view_histories_post FOREIGN KEY (post_id) REFERENCES posts (post_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_post_view_histories_viewer FOREIGN KEY (viewer_user_id) REFERENCES users (user_id) ON DELETE CASCADE;

ALTER TABLE post_daily_metrics
    ADD CONSTRAINT fk_post_daily_metrics_post FOREIGN KEY (post_id) REFERENCES posts (post_id) ON DELETE CASCADE;

ALTER TABLE post_shares
    ADD CONSTRAINT fk_post_shares_post FOREIGN KEY (post_id) REFERENCES posts (post_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_post_shares_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE SET NULL;

ALTER TABLE reports
    ADD CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_reports_post FOREIGN KEY (post_id) REFERENCES posts (post_id) ON DELETE CASCADE;

ALTER TABLE saved_routes
    ADD CONSTRAINT fk_saved_routes_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE;

ALTER TABLE follows
    ADD CONSTRAINT fk_follows_follower FOREIGN KEY (follower_user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_follows_following FOREIGN KEY (following_user_id) REFERENCES users (user_id) ON DELETE CASCADE;

ALTER TABLE user_blocks
    ADD CONSTRAINT fk_user_blocks_blocker FOREIGN KEY (blocker_user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_user_blocks_blocked FOREIGN KEY (blocked_user_id) REFERENCES users (user_id) ON DELETE CASCADE;

ALTER TABLE events
    ADD CONSTRAINT fk_events_sigungu FOREIGN KEY (sigungu_code) REFERENCES sigungu_master (sigungu_code) ON DELETE RESTRICT;

ALTER TABLE ai_recommendation_jobs
    ADD CONSTRAINT fk_ai_recommendation_jobs_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_ai_recommendation_jobs_target_trip FOREIGN KEY (target_trip_id) REFERENCES trips (trip_id) ON DELETE SET NULL;

ALTER TABLE ai_trip_previews
    ADD CONSTRAINT fk_ai_trip_previews_job FOREIGN KEY (job_id) REFERENCES ai_recommendation_jobs (job_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_ai_trip_previews_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_ai_trip_previews_applied_trip FOREIGN KEY (applied_trip_id) REFERENCES trips (trip_id) ON DELETE SET NULL;

ALTER TABLE ai_preview_hashtags
    ADD CONSTRAINT fk_ai_preview_hashtags_preview FOREIGN KEY (preview_id) REFERENCES ai_trip_previews (preview_id) ON DELETE CASCADE;

ALTER TABLE ai_preview_days
    ADD CONSTRAINT fk_ai_preview_days_preview FOREIGN KEY (preview_id) REFERENCES ai_trip_previews (preview_id) ON DELETE CASCADE;

ALTER TABLE ai_preview_places
    ADD CONSTRAINT fk_ai_preview_places_preview FOREIGN KEY (preview_id) REFERENCES ai_trip_previews (preview_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_ai_preview_places_preview_day FOREIGN KEY (preview_id, preview_day_id) REFERENCES ai_preview_days (preview_id, preview_day_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_ai_preview_places_place FOREIGN KEY (place_id) REFERENCES places (place_id) ON DELETE RESTRICT;
