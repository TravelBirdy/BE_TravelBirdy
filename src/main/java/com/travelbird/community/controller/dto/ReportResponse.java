package com.travelbird.community.controller.dto;

import java.time.LocalDateTime;

public record ReportResponse(
        Long reportId,
        String status,
        LocalDateTime createdAt
) {
}
