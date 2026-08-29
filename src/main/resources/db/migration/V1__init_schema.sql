-- V1__init_schema.sql

CREATE TABLE users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255),
    nickname VARCHAR(10),
    introduction VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    role VARCHAR(30) NOT NULL,
    onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE,
    bird_type VARCHAR(30),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'WITHDRAWN')),
    CONSTRAINT ck_users_role CHECK (role IN ('ROLE_USER', 'ROLE_ADMIN'))
);

CREATE TABLE sigungu_master (
    sigungu_code VARCHAR(5) PRIMARY KEY,
    sigungu_name VARCHAR(255) NOT NULL
);

CREATE TABLE files (
    file_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    object_key VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    width INT NOT NULL,
    height INT NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_files_object_key UNIQUE (object_key),
    CONSTRAINT fk_files_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE RESTRICT
);

CREATE TABLE trips (
    trip_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary CLOB,
    sigungu_code VARCHAR(5) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    companion_type VARCHAR(30) NOT NULL,
    pace VARCHAR(30) NOT NULL,
    visibility VARCHAR(30) NOT NULL DEFAULT 'PRIVATE',
    cancelled_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_trips_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_trips_sigungu FOREIGN KEY (sigungu_code) REFERENCES sigungu_master(sigungu_code) ON DELETE RESTRICT
);

CREATE INDEX idx_trips_user_start ON trips (user_id, start_date, trip_id);
CREATE INDEX idx_trips_user_end ON trips (user_id, end_date, trip_id);
CREATE INDEX idx_trips_sigungu ON trips (sigungu_code);

CREATE TABLE posts (
    post_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    title VARCHAR(50),
    content CLOB,
    representative_file_id BIGINT,
    status VARCHAR(30) NOT NULL,
    visibility VARCHAR(30) NOT NULL,
    view_count BIGINT NOT NULL DEFAULT 0,
    save_count BIGINT NOT NULL DEFAULT 0,
    share_count BIGINT NOT NULL DEFAULT 0,
    published_at TIMESTAMP,
    deleted_at TIMESTAMP,
    active_trip_id BIGINT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_posts_active_trip UNIQUE (active_trip_id),
    CONSTRAINT fk_posts_trip FOREIGN KEY (trip_id) REFERENCES trips(trip_id) ON DELETE RESTRICT,
    CONSTRAINT fk_posts_representative_file FOREIGN KEY (representative_file_id) REFERENCES files(file_id) ON DELETE SET NULL
);

CREATE INDEX idx_posts_community ON posts (status, visibility, published_at, post_id);
CREATE INDEX idx_posts_trip_deleted ON posts (trip_id, deleted_at);

CREATE TABLE user_social_accounts (
    user_id BIGINT NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    CONSTRAINT pk_user_social_provider_user PRIMARY KEY (provider, provider_user_id),
    CONSTRAINT uk_user_social_user_provider UNIQUE (user_id, provider),
    CONSTRAINT fk_user_social_accounts_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE refresh_tokens (
    user_id BIGINT PRIMARY KEY,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE events (
    event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tour_api_content_id VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    sigungu_code VARCHAR(5) NOT NULL,
    place_name VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    thumbnail_url VARCHAR(2048),
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_events_sigungu FOREIGN KEY (sigungu_code) REFERENCES sigungu_master(sigungu_code) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uk_events_tour_api_content_id ON events (tour_api_content_id);
CREATE INDEX idx_events_period ON events (start_date, end_date, event_id);
CREATE INDEX idx_events_sigungu_period ON events (sigungu_code, start_date, event_id);

