package com.travelbird.community.service;

import com.travelbird.community.controller.dto.CommunityPeriod;
import com.travelbird.community.controller.dto.CommunityPostPageResponse;
import com.travelbird.community.repository.CommunityPopularFeedRepository;
import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.post.domain.Post;
import com.travelbird.post.domain.PostStatus;
import com.travelbird.post.domain.PostVisibility;
import com.travelbird.post.repository.PostRepository;
import com.travelbird.post.service.CommunityPostCardAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 커뮤니티 전체(ALL)·인기(POPULAR) 목록. backend-functional-spec-v10.md §3.9.1/§3.9.2.
 * FOLLOWING 탭과 차단 필터링은 이번 phase 범위 밖(작성자를 알 방법이 없음 —
 * {@code TripPostReader} 필요, PR 리뷰 요청 참고).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityFeedService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;
    private static final List<PostVisibility> VISIBLE_TO_COMMUNITY =
            List.of(PostVisibility.PUBLIC, PostVisibility.MEMO_PRIVATE);

    private final PostRepository postRepository;
    private final CommunityPopularFeedRepository communityPopularFeedRepository;
    private final CommunityPostCardAssembler cardAssembler;

    public CommunityPostPageResponse listAll(Long cursorOrNull, Integer sizeOrNull, Long viewerIdOrNull) {
        int size = clampSize(sizeOrNull);
        Pageable pageable = PageRequest.of(0, size + 1);

        List<Post> page = (cursorOrNull == null)
                ? postRepository.findAllFeedFirstPage(PostStatus.PUBLISHED, VISIBLE_TO_COMMUNITY, pageable)
                : findAllFeedAfterCursor(cursorOrNull, pageable);

        return toPageResponse(page, size);
    }

    private List<Post> findAllFeedAfterCursor(Long cursorPostId, Pageable pageable) {
        Post cursorPost = postRepository.findById(cursorPostId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CURSOR));
        return postRepository.findAllFeedAfterCursor(
                PostStatus.PUBLISHED, VISIBLE_TO_COMMUNITY,
                roundToStoredPrecision(cursorPost.getPublishedAt()), cursorPostId, pageable);
    }

    /**
     * {@code posts.published_at}은 MySQL {@code DATETIME}(초 단위)이라 저장 시 나노초를
     * 반올림한다(place.service.SavedPlaceService.roundToStoredPrecision과 동일 이슈 —
     * 0.665740초 -> 저장값 +1초). 이 트랜잭션 안에서 방금 만든 Post는 Hibernate 1차
     * 캐시가 삽입 전(반올림 전) 값을 그대로 들고 있어서, 커서 비교 전에 반올림해야
     * DB 저장값과 정확히 맞는다.
     */
    private LocalDateTime roundToStoredPrecision(LocalDateTime value) {
        LocalDateTime truncated = value.truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        return value.getNano() >= 500_000_000 ? truncated.plusSeconds(1) : truncated;
    }

    public CommunityPostPageResponse listPopular(CommunityPeriod periodOrNull, Long cursorOrNull,
                                                  Integer sizeOrNull, Long viewerIdOrNull) {
        CommunityPeriod period = periodOrNull == null ? CommunityPeriod.MONTH : periodOrNull;
        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = resolveFrom(period, to);
        int size = clampSize(sizeOrNull);
        Pageable pageable = PageRequest.of(0, size + 1);

        Long cursorScore = null;
        LocalDateTime cursorPublishedAt = null;
        if (cursorOrNull != null) {
            CommunityPopularFeedRepository.PopularCursorSeed seed = communityPopularFeedRepository
                    .findScoreAndPublishedAtByPostId(cursorOrNull, from, to)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CURSOR));
            cursorScore = seed.getScore();
            cursorPublishedAt = seed.getPublishedAt();
        }

        List<Long> orderedIds = communityPopularFeedRepository.findPopularFeedPostIds(
                from, to, cursorScore, cursorPublishedAt, cursorOrNull, pageable);

        Map<Long, Post> postsById = new HashMap<>();
        postRepository.findAllById(orderedIds).forEach(post -> postsById.put(post.getPostId(), post));
        List<Post> orderedPosts = orderedIds.stream()
                .map(postsById::get)
                .filter(Objects::nonNull)
                .toList();

        return toPageResponse(orderedPosts, size);
    }

    private LocalDateTime resolveFrom(CommunityPeriod period, LocalDateTime now) {
        return switch (period) {
            case WEEK -> now.minusDays(7);
            case MONTH -> now.minusDays(30);
            case SEASON -> now.minusMonths(3);
            case YEAR -> LocalDate.of(now.getYear(), 1, 1).atStartOfDay();
        };
    }

    private CommunityPostPageResponse toPageResponse(List<Post> page, int size) {
        boolean hasNext = page.size() > size;
        List<Post> content = hasNext ? page.subList(0, size) : page;
        Long nextCursor = hasNext ? content.get(content.size() - 1).getPostId() : null;
        return new CommunityPostPageResponse(cardAssembler.toCards(content), nextCursor);
    }

    private int clampSize(Integer sizeOrNull) {
        if (sizeOrNull == null) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(Math.max(sizeOrNull, 1), MAX_PAGE_SIZE);
    }
}
