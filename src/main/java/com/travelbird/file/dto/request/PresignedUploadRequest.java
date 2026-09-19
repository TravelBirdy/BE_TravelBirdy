package com.travelbird.file.dto.request;

import com.travelbird.file.domain.FilePurpose;

/**
 * {@code POST /api/files/presigned-uploads} 요청. 기능명세서 3.5.1.
 * 최종 파일은 항상 {@code image/webp}, 500KB 이하, 가로 1080px 이하다 — 상한 검증은 전용
 * ErrorCode(UNSUPPORTED_FILE_TYPE/FILE_TOO_LARGE/IMAGE_DIMENSION_LIMIT_EXCEEDED)가 필요해서
 * Bean Validation이 아니라 서비스 로직에서 처리한다(이 프로젝트의 기존 검증 방식과 동일).
 */
public record PresignedUploadRequest(
        String fileName,
        String contentType,
        Long sizeBytes,
        Integer width,
        Integer height,
        FilePurpose purpose
) {
}
