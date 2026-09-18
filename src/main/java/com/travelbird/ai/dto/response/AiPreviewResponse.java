package com.travelbird.ai.dto.response;
import com.travelbird.ai.entity.AiPreviewRetentionStatus;
import java.time.LocalDateTime;
import java.util.List;
public record AiPreviewResponse(Long previewId, AiPreviewRetentionStatus retentionStatus,
    boolean editable, String tripTitle, String summary, List<String> hashtags,
    List<AiPreviewDayResponse> days, LocalDateTime createdAt, LocalDateTime updatedAt,
    LocalDateTime expiresAt, LocalDateTime savedAt, long version) {}
