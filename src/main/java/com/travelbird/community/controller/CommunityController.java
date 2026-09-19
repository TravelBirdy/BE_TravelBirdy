package com.travelbird.community.controller;

import com.travelbird.community.controller.dto.CommunityPeriod;
import com.travelbird.community.controller.dto.CommunityPostPageResponse;
import com.travelbird.community.controller.dto.CommunityTab;
import com.travelbird.community.service.CommunityFeedService;
import com.travelbird.community.service.CommunityPostSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 커뮤니티 목록/검색. backend-functional-spec-v10.md §3.9.1~§3.9.3. 비로그인도 허용되는
 * 엔드포인트라({@code security: [{}, {bearerAuth}]}) {@code SecurityUtils.getCurrentUserId()}
 * (없으면 401 throw)를 못 쓴다 — 이 컨트롤러 안에서만 쓰는 로컬 옵션 인증 헬퍼를 둔다.
 * {@code global.security.SecurityUtils}에 {@code getCurrentUserIdOrNull()}로 올릴지는
 * 리뷰에서 결정.
 */
@RestController
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityFeedService communityFeedService;
    private final CommunityPostSearchService communityPostSearchService;

    @GetMapping("/api/community/posts")
    public CommunityPostPageResponse list(@RequestParam CommunityTab tab,
                                           @RequestParam(required = false) CommunityPeriod period,
                                           @RequestParam(required = false) Long cursor,
                                           @RequestParam(required = false) Integer size) {
        Long viewerIdOrNull = currentUserIdOrNull();
        return switch (tab) {
            case ALL -> communityFeedService.listAll(cursor, size, viewerIdOrNull);
            case POPULAR -> communityFeedService.listPopular(period, cursor, size, viewerIdOrNull);
        };
    }

    @GetMapping("/api/community/posts/search")
    public CommunityPostPageResponse search(@RequestParam String query,
                                             @RequestParam(required = false) List<String> sigunguCodes,
                                             @RequestParam(required = false) String theme,
                                             @RequestParam(required = false) Long cursor,
                                             @RequestParam(required = false) Integer size) {
        return communityPostSearchService.search(query, sigunguCodes, theme, cursor, size);
    }

    private Long currentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Long userId)) {
            return null;
        }
        return userId;
    }
}
