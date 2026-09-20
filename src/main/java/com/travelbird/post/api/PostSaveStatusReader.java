package com.travelbird.post.api;

import java.util.List;
import java.util.Set;

/**
 * {@code CommunityPostCardAssembler}가 카드의 {@code savedRoute} 배지를 채우는 데 쓰는
 * SavedRoute(Phase5) 조회 계약. Post와 SavedRoute 모두 Part3 소유라 별도 파트 간 계약은
 * 아니지만, {@code post} 패키지가 {@code savedroute} 패키지를 직접 참조하는 순환 의존을
 * 피하려고 인터페이스로 경계를 둔다.
 */
public interface PostSaveStatusReader {

    /**
     * {@code postIds} 중 {@code userId}가 실제로 저장한(활성 상태) postId만 반환한다.
     * {@code viewerIdOrNull}이 {@code null}이면(비로그인) 항상 빈 집합이다.
     */
    Set<Long> findSavedPostIds(Long viewerIdOrNull, List<Long> postIds);
}
