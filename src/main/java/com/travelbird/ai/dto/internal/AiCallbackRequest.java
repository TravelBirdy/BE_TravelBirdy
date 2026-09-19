package com.travelbird.ai.dto.internal;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record AiCallbackRequest(Long jobId, String status, String tripTitle, String summary,
    List<AiResultDay> days, String requestId, String schemaVersion, String code, String message,
    OffsetDateTime timestamp, Map<String, Object> details) {}
