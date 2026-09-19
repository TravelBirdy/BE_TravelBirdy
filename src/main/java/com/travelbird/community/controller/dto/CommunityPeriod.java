package com.travelbird.community.controller.dto;

/** POPULAR 탭 기간 필터. backend-functional-spec-v10.md §3.9.2. 생략 시 기본값 {@code MONTH}. */
public enum CommunityPeriod {
    WEEK,
    MONTH,
    SEASON,
    YEAR
}
