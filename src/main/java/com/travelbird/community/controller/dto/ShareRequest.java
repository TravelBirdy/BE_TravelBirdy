package com.travelbird.community.controller.dto;

/** {@code channel}을 문자열로 받는다 — enum 바인딩 실패(Spring 기본 400)가 아니라
 * {@code INVALID_SHARE_CHANNEL}(§3.9.5 명시된 전용 코드)로 응답해야 해서 서비스에서 직접 파싱한다. */
public record ShareRequest(String channel) {
}
