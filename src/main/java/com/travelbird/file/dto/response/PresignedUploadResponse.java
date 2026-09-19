package com.travelbird.file.dto.response;

import java.time.LocalDateTime;

/**
 * {@code POST /api/files/presigned-uploads} 응답. 기능명세서 3.5.1.
 * {@code expiresAt}은 이 파일이 연결 안 되면 배치로 삭제되는 시각이다(생성 시점 + 24시간,
 * {@code file.pending-file.expiry-hours}) — Presigned URL 자체의 서명 유효기간(더 짧게,
 * {@code file.presigned-upload.validity-minutes})과는 다른 값이다.
 */
public record PresignedUploadResponse(
        Long fileId,
        String uploadUrl,
        String objectKey,
        LocalDateTime expiresAt
) {
}
