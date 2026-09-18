package com.travelbird.social.service;

import com.travelbird.common.util.PersonalityProfiles;
import com.travelbird.social.dto.response.BlockedUserItem;
import com.travelbird.social.dto.response.BlockedUsersResponse;
import com.travelbird.social.dto.response.FollowListResponse;
import com.travelbird.social.dto.response.FollowUserItem;
import com.travelbird.social.domain.Follow;
import com.travelbird.social.domain.FollowId;
import com.travelbird.user.domain.User;
import com.travelbird.social.domain.UserBlock;
import com.travelbird.social.domain.UserBlockId;
import com.travelbird.social.domain.FollowListType;
import com.travelbird.common.enums.PersonalityTrait;
import com.travelbird.user.domain.UserStatus;
import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.social.repository.FollowRepository;
import com.travelbird.social.repository.UserBlockRepository;
import com.travelbird.user.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SocialService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final UserBlockRepository userBlockRepository;

    public SocialService(
            UserRepository userRepository,
            FollowRepository followRepository,
            UserBlockRepository userBlockRepository
    ) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.userBlockRepository = userBlockRepository;
    }

    public void follow(Long currentUserId, Long targetUserId) {
        if (Objects.equals(currentUserId, targetUserId)) {
            throw new BusinessException(ErrorCode.CANNOT_FOLLOW_SELF);
        }
        User current = getActiveUser(currentUserId);
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (userBlockRepository.existsEitherDirection(currentUserId, targetUserId)) {
            throw new BusinessException(ErrorCode.CANNOT_FOLLOW_BLOCKED_USER);
        }

        if (followRepository.existsById(new FollowId(currentUserId, targetUserId))) {
            return;
        }
        followRepository.save(Follow.create(current, target));
    }

    public void unfollow(Long currentUserId, Long targetUserId) {
        getActiveUser(currentUserId);
        followRepository.deleteByFollowerAndFollowing(currentUserId, targetUserId);
    }

    @Transactional(readOnly = true)
    public FollowListResponse getFollowList(Long userId, FollowListType type, Long cursor, Integer size) {
        int pageSize = resolveSize(size);
        LocalDateTime cursorTime = decodeCursor(cursor);
        Pageable pageable = PageRequest.of(0, pageSize + 1);

        List<Follow> rows = type == FollowListType.FOLLOWERS
                ? followRepository.findFollowers(userId, cursorTime, pageable)
                : followRepository.findFollowings(userId, cursorTime, pageable);

        boolean hasMore = rows.size() > pageSize;
        List<Follow> page = hasMore ? rows.subList(0, pageSize) : rows;

        List<FollowUserItem> items = page.stream()
                .map(follow -> toFollowUserItem(follow, type))
                .toList();
        Long nextCursor = hasMore ? encodeCursor(page.get(page.size() - 1).getFollowedAt()) : null;

        return new FollowListResponse(items, nextCursor);
    }

    public void block(Long currentUserId, Long targetUserId) {
        if (Objects.equals(currentUserId, targetUserId)) {
            throw new BusinessException(ErrorCode.CANNOT_BLOCK_SELF);
        }
        User current = getActiveUser(currentUserId);
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!userBlockRepository.existsById(new UserBlockId(currentUserId, targetUserId))) {
            userBlockRepository.save(UserBlock.create(current, target));
        }
        followRepository.deleteByFollowerAndFollowing(currentUserId, targetUserId);
        followRepository.deleteByFollowerAndFollowing(targetUserId, currentUserId);
    }

    public void unblock(Long currentUserId, Long targetUserId) {
        if (Objects.equals(currentUserId, targetUserId)) {
            throw new BusinessException(ErrorCode.CANNOT_BLOCK_SELF);
        }
        getActiveUser(currentUserId);
        if (!userRepository.existsById(targetUserId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        userBlockRepository.deleteByBlockerAndBlocked(currentUserId, targetUserId);
    }

    @Transactional(readOnly = true)
    public BlockedUsersResponse getBlockedUsers(Long userId, Long cursor, Integer size) {
        int pageSize = resolveSize(size);
        LocalDateTime cursorTime = decodeCursor(cursor);
        Pageable pageable = PageRequest.of(0, pageSize + 1);

        List<UserBlock> rows = userBlockRepository.findBlockedByBlocker(userId, cursorTime, pageable);
        boolean hasMore = rows.size() > pageSize;
        List<UserBlock> page = hasMore ? rows.subList(0, pageSize) : rows;

        List<BlockedUserItem> items = page.stream()
                .map(block -> new BlockedUserItem(
                        block.getBlocked().getUserId(), block.getBlocked().getNickname(), block.getBlocked().getBirdType()))
                .toList();
        Long nextCursor = hasMore ? encodeCursor(page.get(page.size() - 1).getBlockedAt()) : null;

        return new BlockedUsersResponse(items, nextCursor);
    }

    private FollowUserItem toFollowUserItem(Follow follow, FollowListType type) {
        User counterpart = type == FollowListType.FOLLOWERS ? follow.getFollower() : follow.getFollowing();
        PersonalityTrait trait = PersonalityProfiles.traitFor(counterpart.getBirdType());
        return new FollowUserItem(
                counterpart.getUserId(), counterpart.getNickname(), counterpart.getBirdType(), trait, follow.getFollowedAt());
    }

    private int resolveSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return Math.min(Math.max(size, 1), MAX_SIZE);
    }

    private LocalDateTime decodeCursor(Long cursor) {
        return cursor == null ? null : LocalDateTime.ofInstant(Instant.ofEpochMilli(cursor), ZoneOffset.UTC);
    }

    private Long encodeCursor(LocalDateTime time) {
        return time.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    private User getActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_ACTIVE));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_NOT_ACTIVE);
        }
        return user;
    }
}
