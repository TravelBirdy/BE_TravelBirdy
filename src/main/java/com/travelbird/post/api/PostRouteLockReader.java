package com.travelbird.post.api;

/**
 * Part 3 → Part 2(Trip 수정/장소 추가·삭제/순서변경/AI 적용/여행취소) 공개 계약.
 *
 * <p>잠금 기준: {@code publishedAt IS NOT NULL AND deletedAt IS NULL}. visibility가 PRIVATE이거나
 * status가 BLOCKED여도 삭제되지 않았으면 잠금을 유지한다. DRAFT만 있으면 잠금이 없다.
 * Trip에 별도 {@code route_locked} 컬럼을 두지 않고, 잠금은 항상 Post 상태에서 파생시킨다.
 * (공통협의 4.3절 PostRouteLockReader, 5절 순환참조 방지)
 */
public interface PostRouteLockReader {

	/**
	 * 해당 Trip에 연결된 활성(삭제되지 않은) 발행 Post가 있는지 확인한다.
	 */
	PostRouteLock findActivePublishedPostByTripId(Long tripId);
}
