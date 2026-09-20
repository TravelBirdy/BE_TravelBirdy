package com.travelbird.post.domain;

/**
 * 게시글 공개 범위. backend-functional-spec-v10.md §3.8.2.
 *
 * <p>임시 위치: Part2 PR#5의 {@code common.enums.Visibility}와 같은 개념이지만 아직
 * 공통 타입 소유 패키지가 확정되지 않아(그 PR도 미merge) 팀 컨벤션대로 각자 임시로
 * 정의한다. 위치가 확정되면 그쪽으로 옮긴다.
 */
public enum PostVisibility {
    PUBLIC,
    MEMO_PRIVATE,
    PRIVATE
}
