package com.travelbird.global.security;

import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    /**
     * 비로그인도 허용되는 엔드포인트(예: 커뮤니티 목록, 조회수 증가)에서 쓴다.
     * {@link #getCurrentUserId()}와 달리 인증이 없어도 예외를 던지지 않고 {@code null}을
     * 반환한다.
     */
    public static Long getCurrentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof Long userId)) {
            return null;
        }
        return userId;
    }
}
