ALTER TABLE trips
    MODIFY created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    MODIFY updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

CREATE TABLE trip_themes (
    trip_id BIGINT NOT NULL,
    theme VARCHAR(30) NOT NULL,
    PRIMARY KEY (trip_id, theme),
    CONSTRAINT fk_trip_themes_trip FOREIGN KEY (trip_id) REFERENCES trips(trip_id) ON DELETE CASCADE,
    CONSTRAINT ck_trip_themes_theme CHECK (theme IN ('ACTIVITY','SNS_HOTPLACE','NATURE','ATTRACTION','SHOPPING','FOOD'))
);

CREATE TABLE trip_hashtags (
    trip_id BIGINT NOT NULL,
    hashtag VARCHAR(10) NOT NULL,
    PRIMARY KEY (trip_id, hashtag),
    CONSTRAINT fk_trip_hashtags_trip FOREIGN KEY (trip_id) REFERENCES trips(trip_id) ON DELETE CASCADE
);

CREATE TABLE trip_days (
    trip_day_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    day_number INT NOT NULL,
    CONSTRAINT uk_trip_day_number UNIQUE (trip_id, day_number),
    CONSTRAINT uk_trip_day_trip_pair UNIQUE (trip_id, trip_day_id),
    CONSTRAINT fk_trip_days_trip FOREIGN KEY (trip_id) REFERENCES trips(trip_id) ON DELETE CASCADE
);

CREATE TABLE trip_places (
    trip_place_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    trip_day_id BIGINT NOT NULL,
    place_id BIGINT NOT NULL,
    visit_order INT NOT NULL,
    memo VARCHAR(100),
    reason VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_trip_place_global UNIQUE (trip_id, place_id),
    CONSTRAINT uk_trip_day_visit_order UNIQUE (trip_day_id, visit_order),
    INDEX idx_trip_place_trip_day (trip_id, trip_day_id),
    CONSTRAINT fk_trip_places_trip FOREIGN KEY (trip_id) REFERENCES trips(trip_id) ON DELETE CASCADE,
    CONSTRAINT fk_trip_places_trip_day FOREIGN KEY (trip_id, trip_day_id) REFERENCES trip_days(trip_id, trip_day_id) ON DELETE CASCADE,
    CONSTRAINT fk_trip_places_place FOREIGN KEY (place_id) REFERENCES places(place_id) ON DELETE RESTRICT
);

CREATE TABLE trip_place_images (
    trip_place_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    display_order INT NOT NULL,
    PRIMARY KEY (trip_place_id, file_id),
    CONSTRAINT uk_trip_place_image_order UNIQUE (trip_place_id, display_order),
    CONSTRAINT uk_trip_place_image_file UNIQUE (file_id),
    CONSTRAINT fk_trip_place_images_place FOREIGN KEY (trip_place_id) REFERENCES trip_places(trip_place_id) ON DELETE CASCADE,
    CONSTRAINT fk_trip_place_images_file FOREIGN KEY (file_id) REFERENCES files(file_id) ON DELETE RESTRICT
);

CREATE TABLE trip_wishlist_places (
    trip_id BIGINT NOT NULL,
    place_id BIGINT NOT NULL,
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (trip_id, place_id),
    CONSTRAINT fk_trip_wishlist_trip FOREIGN KEY (trip_id) REFERENCES trips(trip_id) ON DELETE CASCADE,
    CONSTRAINT fk_trip_wishlist_place FOREIGN KEY (place_id) REFERENCES places(place_id) ON DELETE CASCADE
);
