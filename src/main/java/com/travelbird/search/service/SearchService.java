package com.travelbird.search.service;

import com.travelbird.common.dto.RegionSummary;
import com.travelbird.community.service.CommunitySearchValidator;
import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.post.api.CommunityPostCard;
import com.travelbird.home.dto.response.PlaceSummary;
import com.travelbird.place.service.PlaceSearchService;
import com.travelbird.region.service.RegionSearchService;
import com.travelbird.search.controller.dto.SearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 통합검색(§3.16.1) 오케스트레이터. 섹션(장소·기록·지역)마다 별도 빈이 각자 읽기 트랜잭션을
 * 열기 때문에 이 클래스는 {@code @Transactional}을 쓰지 않는다 — 한 섹션의 DB 예외가 다른
 * 섹션의 트랜잭션을 rollback-only로 만들지 않고, 실패한 섹션만 빈 목록과
 * {@code unavailableSections}로 빠진다.
 *
 * <p>{@code sigunguCodes}는 장소·지역에만 적용한다. 게시글은 커뮤니티 검색과 같은 이유(트립 지역을
 * 역으로 찾는 조회가 Part2 계약에 없음)로 지역 필터를 걸지 못한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private static final int MAX_LIMIT_PER_TYPE = 5;

    private final CommunitySearchValidator searchValidator;
    private final PlaceSearchService placeSearchService;
    private final PostSearchSectionService postSearchSectionService;
    private final RegionSearchService regionSearchService;

    public SearchResponse search(String rawQuery, Integer limitPerTypeOrNull,
                                  List<String> sigunguCodesOrNull, Long viewerId) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (query.isEmpty()) {
            throw new BusinessException(ErrorCode.SEARCH_QUERY_REQUIRED);
        }
        searchValidator.validateQueryLength(query);

        int limit = clampLimit(limitPerTypeOrNull);
        List<String> sigunguCodes = searchValidator.resolveValidSigunguCodes(sigunguCodesOrNull);
        if (sigunguCodes != null && sigunguCodes.isEmpty()) {
            // "빈 배열은 빈 결과" — 유효한 지역이 하나도 없으면 어느 섹션도 결과가 없다.
            return new SearchResponse(List.of(), List.of(), List.of(), false, false, false, List.of());
        }

        List<String> unavailableSections = new ArrayList<>();
        // 다음 페이지가 있는지 알기 위해 limit+1건을 조회한다.
        List<PlaceSummary> places = section("PLACES", unavailableSections,
                () -> placeSearchService.search(query, sigunguCodes, viewerId, limit + 1));
        List<CommunityPostCard> posts = section("POSTS", unavailableSections,
                () -> postSearchSectionService.search(query, viewerId, limit + 1));
        List<RegionSummary> regions = section("REGIONS", unavailableSections,
                () -> regionSearchService.search(query, sigunguCodes, limit + 1));

        return new SearchResponse(
                head(places, limit), head(posts, limit), head(regions, limit),
                places.size() > limit, posts.size() > limit, regions.size() > limit,
                unavailableSections);
    }

    private <T> List<T> section(String name, List<String> unavailableSections, Supplier<List<T>> loader) {
        try {
            return loader.get();
        } catch (RuntimeException e) {
            log.warn("통합검색 섹션 실패: {}", name, e);
            unavailableSections.add(name);
            return List.of();
        }
    }

    private <T> List<T> head(List<T> items, int limit) {
        return items.size() > limit ? items.subList(0, limit) : items;
    }

    private int clampLimit(Integer limitPerTypeOrNull) {
        if (limitPerTypeOrNull == null) {
            return MAX_LIMIT_PER_TYPE;
        }
        return Math.min(Math.max(limitPerTypeOrNull, 1), MAX_LIMIT_PER_TYPE);
    }
}
