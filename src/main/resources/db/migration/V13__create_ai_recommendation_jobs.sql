-- V1 owns this table, its foreign keys and error_message VARCHAR(500).
ALTER TABLE ai_recommendation_jobs
    ADD CONSTRAINT ck_ai_job_request_type
        CHECK (request_type IN ('GENERAL', 'SAVED_PLACES', 'TRIP_WISHLIST')),
    ADD CONSTRAINT ck_ai_job_status
        CHECK (status IN ('QUEUED', 'PROCESSING', 'SUCCEEDED', 'FAILED', 'EXPIRED'));

CREATE INDEX idx_ai_job_user_requested ON ai_recommendation_jobs (user_id, requested_at);
