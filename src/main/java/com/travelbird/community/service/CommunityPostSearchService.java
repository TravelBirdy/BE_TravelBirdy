package com.travelbird.community.service;

import com.querydsl.core.Tuple;
import com.travelbird.community.controller.dto.CommunityPostPageResponse;
import com.travelbird.community.repository.CommunityPostSearchRepository;
import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.post.domain.Post;
import com.travelbird.post.repository.PostRepository;
import com.travelbird.post.service.CommunityPostCardAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 커뮤니티 검색(§3.9.3). 차단 필터링은 {@link CommunityBlockFilter} 참고. {@code
 * sigunguCodes}/{@code theme}는 유효성 검증까지만 하고 실제 결과 좁히기는 반영하지
 * 않는다 — {@code TripPostReader}(PR#5, merge됨)로 게시글 지역은 알 수 있어도, 역방향
 * (지역 코드 -> 그 지역 트립들)으로 찾는 배치 조회 메서드가 계약에 없어서다. PR 리뷰
 * 요청에 명시.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityPostSearchService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int LOWEST_PRIORITY = 4;

    private final CommunityPostSearchRepository searchRepository;
    private final CommunitySearchValidator searchValidator;
    private final CommunityPostCardAssembler cardAssembler;
    private final PostRepository postRepository;
    private final CommunityBlockFilter blockFilter;

    public CommunityPostPageResponse search(String query, List<String> sigunguCodesOrNull, String themeOrNull,
                                             Long cursorOrNull, Integer sizeOrNull, Long viewerIdOrNull) {
        searchValidator.validateQueryLength(query);

        // "빈 배열은 빈 결과" — sigunguCodes가 명시적으로 []이면 region 데이터 없이도
        // 정확히 처리할 수 있는 유일한 경우라 DB 조회 없이 바로 반환한다.
        if (sigunguCodesOrNull != null && sigunguCodesOrNull.isEmpty()) {
            return new CommunityPostPageResponse(List.of(), null);
        }
        // 유효한 코드만 걸러내는 검증까지는 하지만, 아직 실제 필터로는 안 씀(클래스 Javadoc 참고).
        searchValidator.resolveValidSigunguCodes(sigunguCodesOrNull);
        // themeOrNull: TravelTheme 타입이 아직 코드베이스에 없어 검증/필터 없이 무시한다.

        int size = clampSize(sizeOrNull);
        Pageable pageable = PageRequest.of(0, size + 1);
        List<Long> excludedTripIds = blockFilter.resolveExcludedTripIds(viewerIdOrNull);

        List<Post> page = (cursorOrNull == null)
                ? searchRepository.searchFirstPage(query, excludedTripIds, pageable)
                : searchAfterCursor(query, excludedTripIds, cursorOrNull, pageable);

        boolean hasNext = page.size() > size;
        List<Post> content = hasNext ? page.subList(0, size) : page;
        Long nextCursor = hasNext ? content.get(content.size() - 1).getPostId() : null;
        return new CommunityPostPageResponse(cardAssembler.toCards(content), nextCursor);
    }

    private List<Post> searchAfterCursor(String query, List<Long> excludedTripIds, Long cursorPostId, Pageable pageable) {
        int cursorPriority;
        LocalDateTime cursorCreatedAt;

        Tuple seed = searchRepository.findCursorSeed(query, cursorPostId).orElse(null);
        if (seed != null) {
            cursorPriority = seed.get(0, Integer.class);
            cursorCreatedAt = seed.get(1, LocalDateTime.class);
        } else {
            // 커서 postId가 이 query에는 더 이상 매치되지 않는 엣지 케이스(예: 그 사이 본문이
            // 수정됨) — postId 자체가 존재하긴 하면 최하위 우선순위로 취급해 페이지네이션을
            // 이어간다. postId 자체가 아예 없으면(잘못된 입력) 400.
            Post cursorPost = postRepository.findById(cursorPostId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CURSOR));
            cursorPriority = LOWEST_PRIORITY;
            cursorCreatedAt = cursorPost.getCreatedAt();
        }

        return searchRepository.searchAfterCursor(query, excludedTripIds, cursorPriority, cursorCreatedAt, cursorPostId, pageable);
    }

    private int clampSize(Integer sizeOrNull) {
        if (sizeOrNull == null) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(Math.max(sizeOrNull, 1), MAX_PAGE_SIZE);
    }
}
