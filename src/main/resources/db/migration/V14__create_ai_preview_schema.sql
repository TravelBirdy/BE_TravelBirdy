-- V1 owns the Preview tables, foreign keys, unique indexes and retention_status VARCHAR(30).
ALTER TABLE ai_trip_previews
    ADD CONSTRAINT ck_ai_preview_retention
        CHECK (retention_status IN ('TEMPORARY', 'PERMANENT'));
