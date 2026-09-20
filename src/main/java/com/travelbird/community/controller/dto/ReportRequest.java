package com.travelbird.community.controller.dto;

import com.travelbird.community.domain.ReportReasonCode;

/**
 * {@code reasonCode}는 스펙에 전용 에러 코드가 없어 Jackson 기본 enum 바인딩(실패 시 Spring
 * 기본 400)을 그대로 쓴다 — {@code channel}(§3.9.5, 전용 {@code INVALID_SHARE_CHANNEL})과
 * 다른 취급이다.
 */
public record ReportRequest(
        Long postId,
        ReportReasonCode reasonCode,
        String description
) {
}
