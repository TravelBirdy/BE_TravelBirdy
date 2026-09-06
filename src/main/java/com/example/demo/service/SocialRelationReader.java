package com.example.demo.service;

import java.util.List;

/**
 * Part 3의 커뮤니티/FOLLOWING/검색/프로필 노출에서 사용하는 공개 계약.
 * Part 3는 follows, user_blocks Repository를 직접 조회하지 않는다.
 */
public interface SocialRelationReader {

    List<Long> getFollowingUserIds(Long userId);

    boolean isBlockedEitherDirection(Long userId, Long targetUserId);

    List<Long> getBlockedUserIds(Long userId);
}
