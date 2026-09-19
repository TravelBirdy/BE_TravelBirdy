package com.travelbird.community.controller.dto;

/**
 * backend-functional-spec-v10.md §3.9.1 — {@code FOLLOWING}은 이번 phase에서
 * 의도적으로 제외한다. 작성자 판정에 {@code TripPostReader}(Part2, 미merge)가 필요해서
 * 아직 구현 불가하고, enum에 없는 값은 Spring이 자동으로 400을 반환하므로 별도
 * "미구현" 에러 코드를 만들 필요가 없다. {@code TripPostReader}가 merge되면 추가한다.
 */
public enum CommunityTab {
    ALL,
    POPULAR
}
