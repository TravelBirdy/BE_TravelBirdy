package com.travelbird.post.domain;

/**
 * 게시글 상태. backend-functional-spec-v10.md §3.8.
 */
public enum PostStatus {
    DRAFT,
    PUBLISHED,
    BLOCKED
}
