package com.travelbird.post.api;

public record PostRouteLock(
		Long postId,
		boolean routeLocked
) {
}
