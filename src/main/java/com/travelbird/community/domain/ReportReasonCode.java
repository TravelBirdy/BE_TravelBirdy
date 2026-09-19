package com.travelbird.community.domain;

/**
 * 게시글 신고 사유. backend-functional-spec-v10.md §3.9.6.
 *
 * <p>임시 위치: 공통 Enum 소유 패키지가 아직 확정되지 않아(팀 컨벤션대로) 각자 임시로
 * 정의한다. 위치가 확정되면 그쪽으로 옮긴다.
 */
public enum ReportReasonCode {
    SPAM,
    ABUSE,
    INAPPROPRIATE,
    OTHER
}
