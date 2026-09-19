package com.travelbird.community.domain;

/**
 * 신고 처리 상태. backend-functional-spec-v10.md §3.9.6 — 관리자 화면/API가 없어서
 * 이 프로젝트 범위에선 {@code RECEIVED} 외의 값으로 전이시킬 방법이 없다. 운영자가 DB에서
 * {@code posts.status=BLOCKED}로 직접 바꾸는 것과 이 상태는 별개다.
 */
public enum ReportStatus {
    RECEIVED
}
