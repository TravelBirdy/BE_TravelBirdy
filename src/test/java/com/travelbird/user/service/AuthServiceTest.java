package com.travelbird.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.travelbird.user.dto.request.KakaoLoginRequest;
import com.travelbird.user.dto.response.KakaoLoginResponse;
import com.travelbird.user.dto.request.RefreshTokenRequest;
import com.travelbird.user.dto.response.TokenPair;
import com.travelbird.user.client.KakaoApiClient;
import com.travelbird.user.client.KakaoUserInfo;
import com.travelbird.user.domain.RefreshToken;
import com.travelbird.user.domain.User;
import com.travelbird.user.domain.UserSocialAccount;
import com.travelbird.user.domain.UserSocialAccountId;
import com.travelbird.user.domain.UserRole;
import com.travelbird.user.domain.UserStatus;
import com.travelbird.global.error.ApiException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.user.repository.RefreshTokenRepository;
import com.travelbird.user.repository.UserRepository;
import com.travelbird.user.repository.UserSocialAccountRepository;
import com.travelbird.global.security.JwtUtil;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserSocialAccountRepository userSocialAccountRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private KakaoApiClient kakaoApiClient;

    private JwtUtil jwtUtil;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil("test-secret-key-for-jwt-unit-tests-only-32bytes+", 1800L, 2592000L);
        authService = new AuthService(userRepository, userSocialAccountRepository, refreshTokenRepository, kakaoApiClient, jwtUtil);

        org.mockito.Mockito.lenient().when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "userId", 1L);
            return user;
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void kakaoLogin_missingToken_throwsInvalidAuthRequest() {
        assertThatThrownBy(() -> authService.kakaoLogin(new KakaoLoginRequest(" ")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_AUTH_REQUEST);
    }

    @Test
    void kakaoLogin_newUser_createsAccountAndIssuesTokens() {
        when(kakaoApiClient.getUserInfo("valid-token")).thenReturn(new KakaoUserInfo(12345L, "user@kakao.com"));
        when(userSocialAccountRepository.findById(new UserSocialAccountId("KAKAO", "12345"))).thenReturn(Optional.empty());
        when(refreshTokenRepository.findById(1L)).thenReturn(Optional.empty());

        KakaoLoginResponse response = authService.kakaoLogin(new KakaoLoginRequest("valid-token"));

        assertThat(response.isNewUser()).isTrue();
        assertThat(response.onboardingCompleted()).isFalse();
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.accessTokenExpiresIn()).isEqualTo(1800L);
        assertThat(response.refreshTokenExpiresIn()).isEqualTo(2592000L);
        verify(userSocialAccountRepository).save(any(UserSocialAccount.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void kakaoLogin_suspendedUser_throwsUserNotActive() {
        User suspended = existingUserWithStatus(UserStatus.SUSPENDED);
        when(kakaoApiClient.getUserInfo("valid-token")).thenReturn(new KakaoUserInfo(999L, null));
        UserSocialAccount link = UserSocialAccount.create("KAKAO", "999", 1L);
        when(userSocialAccountRepository.findById(new UserSocialAccountId("KAKAO", "999"))).thenReturn(Optional.of(link));
        when(userRepository.findById(1L)).thenReturn(Optional.of(suspended));

        assertThatThrownBy(() -> authService.kakaoLogin(new KakaoLoginRequest("valid-token")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_ACTIVE);
    }

    @Test
    void kakaoLogin_activeSessionExists_throwsConflict() {
        User active = existingUserWithStatus(UserStatus.ACTIVE);
        when(kakaoApiClient.getUserInfo("valid-token")).thenReturn(new KakaoUserInfo(999L, null));
        UserSocialAccount link = UserSocialAccount.create("KAKAO", "999", 1L);
        when(userSocialAccountRepository.findById(new UserSocialAccountId("KAKAO", "999"))).thenReturn(Optional.of(link));
        when(userRepository.findById(1L)).thenReturn(Optional.of(active));
        RefreshToken activeToken = RefreshToken.issue(1L, "hash", LocalDateTime.now().plusDays(1));
        when(refreshTokenRepository.findById(1L)).thenReturn(Optional.of(activeToken));

        assertThatThrownBy(() -> authService.kakaoLogin(new KakaoLoginRequest("valid-token")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACTIVE_SESSION_ALREADY_EXISTS);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refresh_expiredToken_throwsRefreshTokenExpired() {
        JwtUtil shortLivedJwtUtil = new JwtUtil("test-secret-key-for-jwt-unit-tests-only-32bytes+", 1800L, -1L);
        AuthService serviceWithShortLivedTokens = new AuthService(
                userRepository, userSocialAccountRepository, refreshTokenRepository, kakaoApiClient, shortLivedJwtUtil);
        String expiredToken = shortLivedJwtUtil.createRefreshToken(1L, UserRole.ROLE_USER);

        assertThatThrownBy(() -> serviceWithShortLivedTokens.refresh(new RefreshTokenRequest(expiredToken)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.REFRESH_TOKEN_EXPIRED);
    }

    @Test
    void refresh_malformedToken_throwsInvalidRefreshToken() {
        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("not-a-jwt")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    void refresh_validToken_rotatesAndReturnsNewPair() {
        String validRefreshToken = jwtUtil.createRefreshToken(1L, UserRole.ROLE_USER);
        RefreshToken stored = RefreshToken.issue(1L, BCrypt.hashpw(validRefreshToken, BCrypt.gensalt()), LocalDateTime.now().plusDays(1));
        when(refreshTokenRepository.findById(1L)).thenReturn(Optional.of(stored));
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUserWithStatus(UserStatus.ACTIVE)));

        TokenPair tokenPair = authService.refresh(new RefreshTokenRequest(validRefreshToken));

        assertThat(tokenPair.accessToken()).isNotBlank();
        assertThat(tokenPair.refreshToken()).isNotEqualTo(validRefreshToken);
        assertThat(stored.getRevokedAt()).isNull();
    }

    @Test
    void logout_ownerMismatch_throwsTokenAccessDenied() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(2L, null, java.util.List.of()));
        String tokenOwnedByAnotherUser = jwtUtil.createRefreshToken(1L, UserRole.ROLE_USER);

        assertThatThrownBy(() -> authService.logout(new RefreshTokenRequest(tokenOwnedByAnotherUser)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.TOKEN_ACCESS_DENIED);
    }

    @Test
    void logout_ownToken_revokesRefreshToken() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null, java.util.List.of()));
        String ownToken = jwtUtil.createRefreshToken(1L, UserRole.ROLE_USER);
        RefreshToken stored = RefreshToken.issue(1L, "hash", LocalDateTime.now().plusDays(1));
        when(refreshTokenRepository.findById(1L)).thenReturn(Optional.of(stored));

        authService.logout(new RefreshTokenRequest(ownToken));

        assertThat(stored.getRevokedAt()).isNotNull();
    }

    private User existingUserWithStatus(UserStatus status) {
        User user = User.createFromKakao(null);
        ReflectionTestUtils.setField(user, "userId", 1L);
        ReflectionTestUtils.setField(user, "status", status);
        return user;
    }
}
