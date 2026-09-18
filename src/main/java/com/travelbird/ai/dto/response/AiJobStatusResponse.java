package com.travelbird.ai.dto.response;
import com.travelbird.ai.entity.BackendAiJobStatus;import java.time.LocalDateTime;
public record AiJobStatusResponse(Long jobId,BackendAiJobStatus status,Long previewId,boolean previewAvailable,String previewRetentionStatus,boolean editable,LocalDateTime requestedAt,LocalDateTime startedAt,LocalDateTime completedAt,LocalDateTime expiresAt,LocalDateTime expiredAt,LocalDateTime savedAt,AiJobError error){public record AiJobError(String code,String message,boolean retryable){}}
