package com.travelbird.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.travelbird.user.domain.RefreshToken;
import com.travelbird.user.domain.User;
import com.travelbird.user.domain.UserStatus;
import com.travelbird.global.error.ApiException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.file.repository.FileAssetRepository;
import com.travelbird.social.repository.FollowRepository;
import com.travelbird.user.repository.RefreshTokenRepository;
import com.travelbird.social.repository.UserBlockRepository;
import com.travelbird.user.repository.UserRepository;
import com.travelbird.user.repository.UserSocialAccountRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WithdrawalServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private UserSocialAccountRepository userSocialAccountRepository;
    @Mock
    private FollowRepository followRepository;
    @Mock
    private UserBlockRepository userBlockRepository;
    @Mock
    private FileAssetRepository fileAssetRepository;

    private WithdrawalService withdrawalService;

    @BeforeEach
    void setUp() {
        withdrawalService = new WithdrawalService(
                userRepository, refreshTokenRepository, userSocialAccountRepository,
                followRepository, userBlockRepository, fileAssetRepository);
    }

    @Test
    void withdraw_userNotFound_throwsUserNotActive() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> withdrawalService.withdraw(1L))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_ACTIVE);
    }

    @Test
    void withdraw_suspendedUser_throwsUserNotActive() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUserWithStatus(UserStatus.SUSPENDED)));

        assertThatThrownBy(() -> withdrawalService.withdraw(1L))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_ACTIVE);

        verify(fileAssetRepository, never()).deleteAllByUserId(any());
    }

    @Test
    void withdraw_alreadyWithdrawn_isIdempotentNoOp() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUserWithStatus(UserStatus.WITHDRAWN)));

        withdrawalService.withdraw(1L);

        verify(refreshTokenRepository, never()).findById(any());
        verify(fileAssetRepository, never()).deleteAllByUserId(any());
        verify(followRepository, never()).deleteAllInvolvingUser(any());
        verify(userBlockRepository, never()).deleteAllInvolvingUser(any());
        verify(userSocialAccountRepository, never()).deleteAllByUserId(any());
    }

    @Test
    void withdraw_activeUser_revokesTokenCleansUpAndTombstones() {
        User user = existingUserWithStatus(UserStatus.ACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        RefreshToken token = RefreshToken.issue(1L, "hash", LocalDateTime.now().plusDays(1));
        when(refreshTokenRepository.findById(1L)).thenReturn(Optional.of(token));

        withdrawalService.withdraw(1L);

        assertThat(token.getRevokedAt()).isNotNull();
        verify(fileAssetRepository).deleteAllByUserId(1L);
        verify(followRepository).deleteAllInvolvingUser(1L);
        verify(userBlockRepository).deleteAllInvolvingUser(1L);
        verify(userSocialAccountRepository).deleteAllByUserId(1L);

        assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(user.getEmail()).isNull();
        assertThat(user.getNickname()).isNull();
        assertThat(user.getIntroduction()).isNull();
        assertThat(user.getBirdType()).isNull();
        assertThat(user.isOnboardingCompleted()).isFalse();
    }

    private User existingUserWithStatus(UserStatus status) {
        User user = User.createFromKakao("user@kakao.com");
        ReflectionTestUtils.setField(user, "userId", 1L);
        ReflectionTestUtils.setField(user, "nickname", "닉네임");
        ReflectionTestUtils.setField(user, "status", status);
        return user;
    }
}
