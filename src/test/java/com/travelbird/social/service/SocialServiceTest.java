package com.travelbird.social.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.travelbird.social.domain.Follow;
import com.travelbird.social.domain.FollowId;
import com.travelbird.user.domain.User;
import com.travelbird.social.domain.UserBlock;
import com.travelbird.social.domain.UserBlockId;
import com.travelbird.social.domain.FollowListType;
import com.travelbird.user.domain.UserStatus;
import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.social.repository.FollowRepository;
import com.travelbird.social.repository.UserBlockRepository;
import com.travelbird.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SocialServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private FollowRepository followRepository;
    @Mock
    private UserBlockRepository userBlockRepository;

    private SocialService socialService;
    private User me;
    private User other;

    @BeforeEach
    void setUp() {
        socialService = new SocialService(userRepository, followRepository, userBlockRepository);

        me = User.createFromKakao("me@kakao.com");
        ReflectionTestUtils.setField(me, "userId", 1L);
        ReflectionTestUtils.setField(me, "status", UserStatus.ACTIVE);

        other = User.createFromKakao("other@kakao.com");
        ReflectionTestUtils.setField(other, "userId", 2L);
        ReflectionTestUtils.setField(other, "status", UserStatus.ACTIVE);
    }

    @Test
    void follow_self_throwsCannotFollowSelf() {
        assertThatThrownBy(() -> socialService.follow(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CANNOT_FOLLOW_SELF);
    }

    @Test
    void follow_targetNotFound_throwsUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> socialService.follow(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void follow_blockedRelationExists_throwsCannotFollowBlockedUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));
        when(userRepository.findById(2L)).thenReturn(Optional.of(other));
        when(userBlockRepository.existsEitherDirection(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> socialService.follow(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CANNOT_FOLLOW_BLOCKED_USER);

        verify(followRepository, never()).save(any());
    }

    @Test
    void follow_alreadyFollowing_isIdempotentNoOp() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));
        when(userRepository.findById(2L)).thenReturn(Optional.of(other));
        when(userBlockRepository.existsEitherDirection(1L, 2L)).thenReturn(false);
        when(followRepository.existsById(new FollowId(1L, 2L))).thenReturn(true);

        socialService.follow(1L, 2L);

        verify(followRepository, never()).save(any());
    }

    @Test
    void follow_valid_createsFollow() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));
        when(userRepository.findById(2L)).thenReturn(Optional.of(other));
        when(userBlockRepository.existsEitherDirection(1L, 2L)).thenReturn(false);
        when(followRepository.existsById(new FollowId(1L, 2L))).thenReturn(false);

        socialService.follow(1L, 2L);

        verify(followRepository).save(any(Follow.class));
    }

    @Test
    void unfollow_deletesRelation() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));

        socialService.unfollow(1L, 2L);

        verify(followRepository).deleteByFollowerAndFollowing(1L, 2L);
    }

    @Test
    void block_self_throwsCannotBlockSelf() {
        assertThatThrownBy(() -> socialService.block(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CANNOT_BLOCK_SELF);
    }

    @Test
    void block_targetNotFound_throwsUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> socialService.block(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void block_valid_createsBlockAndRemovesFollowBothDirections() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));
        when(userRepository.findById(2L)).thenReturn(Optional.of(other));
        when(userBlockRepository.existsById(new UserBlockId(1L, 2L))).thenReturn(false);

        socialService.block(1L, 2L);

        verify(userBlockRepository).save(any(UserBlock.class));
        verify(followRepository).deleteByFollowerAndFollowing(1L, 2L);
        verify(followRepository).deleteByFollowerAndFollowing(2L, 1L);
    }

    @Test
    void block_alreadyBlocked_doesNotSaveDuplicate() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));
        when(userRepository.findById(2L)).thenReturn(Optional.of(other));
        when(userBlockRepository.existsById(new UserBlockId(1L, 2L))).thenReturn(true);

        socialService.block(1L, 2L);

        verify(userBlockRepository, never()).save(any());
        verify(followRepository).deleteByFollowerAndFollowing(1L, 2L);
    }

    @Test
    void unblock_self_throwsCannotBlockSelf() {
        assertThatThrownBy(() -> socialService.unblock(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CANNOT_BLOCK_SELF);
    }

    @Test
    void unblock_targetNotFound_throwsUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));
        when(userRepository.existsById(2L)).thenReturn(false);

        assertThatThrownBy(() -> socialService.unblock(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void unblock_valid_deletesBlock() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));
        when(userRepository.existsById(2L)).thenReturn(true);

        socialService.unblock(1L, 2L);

        verify(userBlockRepository).deleteByBlockerAndBlocked(1L, 2L);
    }

    @Test
    void getFollowList_moreRowsThanSize_returnsNextCursor() {
        Follow f1 = Follow.create(other, me);
        ReflectionTestUtils.setField(f1, "followedAt", java.time.LocalDateTime.now());
        Follow f2 = Follow.create(me, other);
        ReflectionTestUtils.setField(f2, "followedAt", java.time.LocalDateTime.now());
        when(followRepository.findFollowers(eq(1L), eq(null), any())).thenReturn(List.of(f1, f2));

        var response = socialService.getFollowList(1L, FollowListType.FOLLOWERS, null, 1);

        assertThat(response.items()).hasSize(1);
        assertThat(response.nextCursor()).isNotNull();
    }

    @Test
    void getFollowList_lastPage_returnsNullCursor() {
        Follow f1 = Follow.create(other, me);
        when(followRepository.findFollowers(eq(1L), eq(null), any())).thenReturn(List.of(f1));

        var response = socialService.getFollowList(1L, FollowListType.FOLLOWERS, null, 20);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).userId()).isEqualTo(2L);
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    void getBlockedUsers_returnsItems() {
        UserBlock block = UserBlock.create(me, other);
        when(userBlockRepository.findBlockedByBlocker(eq(1L), eq(null), any())).thenReturn(List.of(block));

        var response = socialService.getBlockedUsers(1L, null, null);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).userId()).isEqualTo(2L);
        assertThat(response.nextCursor()).isNull();
    }
}
