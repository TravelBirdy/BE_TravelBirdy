-- V1 owns the Trip tables, DATETIME columns, foreign keys and unique indexes.
-- Add only the domain constraint missing from that baseline.
ALTER TABLE trip_themes
    ADD CONSTRAINT ck_trip_themes_theme
        CHECK (theme IN ('ACTIVITY', 'SNS_HOTPLACE', 'NATURE', 'ATTRACTION', 'SHOPPING', 'FOOD'));
