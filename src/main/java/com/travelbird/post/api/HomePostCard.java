package com.travelbird.post.api;

import com.travelbird.common.dto.RegionSummary;

public record HomePostCard(
		Long postId,
		String title,
		String thumbnailUrl,
		RegionSummary region
) {
}
