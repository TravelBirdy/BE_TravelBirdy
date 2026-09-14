package com.example.demo.service;

import java.util.List;

/**
 * Part 3의 커뮤니티/FOLLOWING/검색/프로필 노출에서 사용하는 공개 계약.
 * Part 3는 follows, user_blocks Repository를 직접 조회하지 않는다.
 */
public interface SocialRelationReader {

    List<Long> getFollowingUserIds(Long userId);

    boolean isBlockedEitherDirection(Long userId, Long targetUserId);

    /**
     * userId가 차단한 사용자와 userId를 차단한 사용자를 합쳐서 반환한다(양방향, 중복 제거).
     * 기능명세서 3.9.3/3.13.3 "차단 관계가 어느 방향으로든 존재하면 서로의 게시글을 제외한다"
     * 규칙에 따라, 커뮤니티 목록 등에서 여러 작성자를 한 번에 필터링할 때 사용한다.
     */
    List<Long> getBlockedUserIdsEitherDirection(Long userId);
}
