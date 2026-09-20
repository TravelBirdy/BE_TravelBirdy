package com.travelbird.community.controller;

import com.travelbird.community.controller.dto.CommunityPeriod;
import com.travelbird.community.controller.dto.CommunityPostPageResponse;
import com.travelbird.community.controller.dto.CommunityTab;
import com.travelbird.community.service.CommunityFeedService;
import com.travelbird.community.service.CommunityPostSearchService;
import com.travelbird.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 커뮤니티 목록/검색. backend-functional-spec-v10.md §3.9.1~§3.9.3. ALL/POPULAR/검색은
 * 비로그인도 허용되는 엔드포인트라({@code security: [{}, {bearerAuth}]})
 * {@code SecurityUtils.getCurrentUserIdOrNull()}을 쓴다. FOLLOWING은 로그인 필수라
 * {@code getCurrentUserId()}(없으면 401)를 쓴다.
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
        return switch (tab) {
            case ALL -> communityFeedService.listAll(cursor, size, SecurityUtils.getCurrentUserIdOrNull());
            case POPULAR -> communityFeedService.listPopular(period, cursor, size, SecurityUtils.getCurrentUserIdOrNull());
            case FOLLOWING -> communityFeedService.listFollowing(cursor, size, SecurityUtils.getCurrentUserId());
        };
    }

    @GetMapping("/api/community/posts/search")
    public CommunityPostPageResponse search(@RequestParam String query,
                                             @RequestParam(required = false) List<String> sigunguCodes,
                                             @RequestParam(required = false) String theme,
                                             @RequestParam(required = false) Long cursor,
                                             @RequestParam(required = false) Integer size) {
        return communityPostSearchService.search(
                query, sigunguCodes, theme, cursor, size, SecurityUtils.getCurrentUserIdOrNull());
    }
}
