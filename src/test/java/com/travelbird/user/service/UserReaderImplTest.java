package com.travelbird.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.travelbird.common.enums.BirdType;
import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.user.api.UserSummary;
import com.travelbird.user.domain.User;
import com.travelbird.user.domain.UserStatus;
import com.travelbird.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserReaderImplTest {

    @Mock
    private UserRepository userRepository;

    private UserReaderImpl userReader;

    @BeforeEach
    void setUp() {
        userReader = new UserReaderImpl(userRepository);
    }

    private User activeUser(Long userId) {
        User user = User.createFromKakao("user" + userId + "@kakao.com");
        ReflectionTestUtils.setField(user, "userId", userId);
        ReflectionTestUtils.setField(user, "nickname", "새길동");
        ReflectionTestUtils.setField(user, "birdType", BirdType.OMOKNUNI);
        return user;
    }

    @Test
    void validateActiveUser_activeUser_doesNotThrow() {
        User user = activeUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userReader.validateActiveUser(1L);
    }

    @Test
    void validateActiveUser_withdrawnUser_throwsUserNotActive() {
        User user = activeUser(1L);
        user.withdraw();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userReader.validateActiveUser(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_ACTIVE);
    }

    @Test
    void validateActiveUser_userNotFound_throwsUserNotActive() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userReader.validateActiveUser(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_ACTIVE);
    }

    @Test
    void getUserSummary_activeUser_returnsNicknameAndBirdType() {
        User user = activeUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserSummary summary = userReader.getUserSummary(1L);

        assertThat(summary.userId()).isEqualTo(1L);
        assertThat(summary.nickname()).isEqualTo("새길동");
        assertThat(summary.birdType()).isEqualTo(BirdType.OMOKNUNI);
        assertThat(summary.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void getUserSummary_withdrawnUser_returnsNullNicknameAndBirdType() {
        User user = activeUser(1L);
        user.withdraw();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserSummary summary = userReader.getUserSummary(1L);

        assertThat(summary.nickname()).isNull();
        assertThat(summary.birdType()).isNull();
        assertThat(summary.status()).isEqualTo(UserStatus.WITHDRAWN);
    }

    @Test
    void getUserSummary_userNotFound_throwsUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userReader.getUserSummary(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}
