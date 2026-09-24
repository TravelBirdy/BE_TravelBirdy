package com.travelbird.post.controller.dto;

import java.util.List;

/** backend-functional-spec-v10.md §3.8.3. */
public record MyPostsResponse(
        List<MyPostItem> items,
        Long nextCursor
) {
}
