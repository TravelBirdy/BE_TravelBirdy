package com.travelbird.social.service;

import com.travelbird.social.api.SocialRelationReader;
import com.travelbird.social.repository.FollowRepository;
import com.travelbird.social.repository.UserBlockRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class SocialRelationReaderImpl implements SocialRelationReader {

    private final FollowRepository followRepository;
    private final UserBlockRepository userBlockRepository;

    public SocialRelationReaderImpl(FollowRepository followRepository, UserBlockRepository userBlockRepository) {
        this.followRepository = followRepository;
        this.userBlockRepository = userBlockRepository;
    }

    @Override
    public List<Long> getFollowingUserIds(Long userId) {
        return followRepository.findFollowingUserIds(userId);
    }

    @Override
    public boolean isBlockedEitherDirection(Long userId, Long targetUserId) {
        return userBlockRepository.existsEitherDirection(userId, targetUserId);
    }

    @Override
    public List<Long> getBlockedUserIdsEitherDirection(Long userId) {
        return userBlockRepository.findBlockedUserIdsEitherDirection(userId);
    }
}
