package com.travelbird.user.service;

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
import com.travelbird.global.security.TokenType;
import com.travelbird.user.domain.UserStatus;
import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.user.repository.RefreshTokenRepository;
import com.travelbird.user.repository.UserRepository;
import com.travelbird.user.repository.UserSocialAccountRepository;
import com.travelbird.global.security.SecurityUtils;
import com.travelbird.global.security.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class AuthService {

    private static final String KAKAO_PROVIDER = "KAKAO";

    private final UserRepository userRepository;
    private final UserSocialAccountRepository userSocialAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final KakaoApiClient kakaoApiClient;
    private final JwtUtil jwtUtil;

    public AuthService(
            UserRepository userRepository,
            UserSocialAccountRepository userSocialAccountRepository,
            RefreshTokenRepository refreshTokenRepository,
            KakaoApiClient kakaoApiClient,
            JwtUtil jwtUtil
    ) {
        this.userRepository = userRepository;
        this.userSocialAccountRepository = userSocialAccountRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.kakaoApiClient = kakaoApiClient;
        this.jwtUtil = jwtUtil;
    }

    public KakaoLoginResponse kakaoLogin(KakaoLoginRequest request) {
        if (request == null || !StringUtils.hasText(request.kakaoAccessToken())) {
            throw new BusinessException(ErrorCode.INVALID_AUTH_REQUEST);
        }

        KakaoUserInfo kakaoUserInfo = kakaoApiClient.getUserInfo(request.kakaoAccessToken());
        String providerUserId = String.valueOf(kakaoUserInfo.id());
        UserSocialAccountId socialAccountId = new UserSocialAccountId(KAKAO_PROVIDER, providerUserId);

        Optional<UserSocialAccount> existingLink = userSocialAccountRepository.findById(socialAccountId);
        boolean isNewUser = existingLink.isEmpty();

        User user;
        if (isNewUser) {
            user = userRepository.save(User.createFromKakao(kakaoUserInfo.email()));
            userSocialAccountRepository.save(UserSocialAccount.create(KAKAO_PROVIDER, providerUserId, user.getUserId()));
        } else {
            user = userRepository.findById(existingLink.get().getUserId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_ACTIVE));
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_NOT_ACTIVE);
        }

        Optional<RefreshToken> existingToken = refreshTokenRepository.findById(user.getUserId());
        if (existingToken.isPresent() && existingToken.get().isActive()) {
            throw new BusinessException(ErrorCode.ACTIVE_SESSION_ALREADY_EXISTS);
        }

        String accessToken = jwtUtil.createAccessToken(user.getUserId(), user.getRole());
        String refreshToken = jwtUtil.createRefreshToken(user.getUserId(), user.getRole());
        issueOrRotateRefreshToken(user.getUserId(), existingToken, refreshToken);

        return new KakaoLoginResponse(
                accessToken,
                refreshToken,
                jwtUtil.getAccessTokenExpirationSeconds(),
                jwtUtil.getRefreshTokenExpirationSeconds(),
                isNewUser,
                user.isOnboardingCompleted(),
                user.getUserId(),
                user.getNickname(),
                user.getBirdType()
        );
    }

    public TokenPair refresh(RefreshTokenRequest request) {
        if (request == null || !StringUtils.hasText(request.refreshToken())) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Claims claims;
        try {
            claims = jwtUtil.parseClaims(request.refreshToken());
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        if (!TokenType.REFRESH.name().equals(claims.get("tokenType", String.class))) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long userId = claims.get("userId", Long.class);
        RefreshToken stored = refreshTokenRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (stored.getRevokedAt() != null) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_REVOKED);
        }
        if (!stored.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }
        if (!BCrypt.checkpw(request.refreshToken(), stored.getTokenHash())) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_ACTIVE));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_NOT_ACTIVE);
        }

        String newAccessToken = jwtUtil.createAccessToken(userId, user.getRole());
        String newRefreshToken = jwtUtil.createRefreshToken(userId, user.getRole());
        stored.rotate(hash(newRefreshToken), LocalDateTime.now().plusSeconds(jwtUtil.getRefreshTokenExpirationSeconds()));

        return new TokenPair(
                newAccessToken,
                newRefreshToken,
                jwtUtil.getAccessTokenExpirationSeconds(),
                jwtUtil.getRefreshTokenExpirationSeconds()
        );
    }

    public void logout(RefreshTokenRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        if (request == null || !StringUtils.hasText(request.refreshToken())) {
            throw new BusinessException(ErrorCode.TOKEN_ACCESS_DENIED);
        }

        Claims claims;
        try {
            claims = parseClaimsAllowExpired(request.refreshToken());
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.TOKEN_ACCESS_DENIED);
        }

        Long ownerUserId = claims.get("userId", Long.class);
        if (!Objects.equals(ownerUserId, currentUserId)) {
            throw new BusinessException(ErrorCode.TOKEN_ACCESS_DENIED);
        }

        refreshTokenRepository.findById(currentUserId).ifPresent(RefreshToken::revoke);
    }

    private Claims parseClaimsAllowExpired(String token) {
        try {
            return jwtUtil.parseClaims(token);
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    private void issueOrRotateRefreshToken(Long userId, Optional<RefreshToken> existing, String rawRefreshToken) {
        String hash = hash(rawRefreshToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(jwtUtil.getRefreshTokenExpirationSeconds());
        if (existing.isPresent()) {
            existing.get().rotate(hash, expiresAt);
        } else {
            refreshTokenRepository.save(RefreshToken.issue(userId, hash, expiresAt));
        }
    }

    private String hash(String rawToken) {
        return BCrypt.hashpw(rawToken, BCrypt.gensalt());
    }
}
