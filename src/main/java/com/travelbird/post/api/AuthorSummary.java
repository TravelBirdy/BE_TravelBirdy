package com.travelbird.post.api;

/**
 * 게시글 카드에 노출하는 작성자 최소 정보.
 *
 * <p>{@code birdType}은 공통협의 13절 공통 타입 목록에 있는 {@code BirdType}을 참조해야 하지만,
 * 아직 공통 소유 패키지가 확정되지 않아 임시로 {@code String}으로 둔다. 위치가 확정되면
 * 해당 Enum 타입으로 교체한다.
 */
public record AuthorSummary(
		Long userId,
		String nickname,
		String birdType
) {
}
