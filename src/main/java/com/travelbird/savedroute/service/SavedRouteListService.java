package com.travelbird.savedroute.service;

import com.travelbird.common.dto.ImageSummary;
import com.travelbird.common.dto.RegionSummary;
import com.travelbird.common.enums.TravelTheme;
import com.travelbird.file.api.FileLinkService;
import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.place.api.PlaceContract;
import com.travelbird.place.api.PlaceReader;
import com.travelbird.post.api.AuthorSummary;
import com.travelbird.post.domain.Post;
import com.travelbird.post.domain.PostStatus;
import com.travelbird.post.domain.PostVisibility;
import com.travelbird.post.repository.PostRepository;
import com.travelbird.region.api.RegionReader;
import com.travelbird.savedroute.controller.dto.SavedRouteListItem;
import com.travelbird.savedroute.controller.dto.SavedRouteListResponse;
import com.travelbird.savedroute.domain.SavedRoute;
import com.travelbird.savedroute.domain.SavedRouteSourceType;
import com.travelbird.savedroute.repository.SavedRouteRepository;
import com.travelbird.social.api.SocialRelationReader;
import com.travelbird.trip.api.TripPostReader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 저장한 경로 목록 조회. backend-functional-spec-v10.md §3.10.2.
 *
 * <p>{@code sourceType=AI_PREVIEW} 항목은 표시용 데이터(제목/지역/장소/테마)를 주는 Part2
 * 계약이 아직 없어({@code ai.api}엔 저장/취소 계약만 있음) title/region/placeNames/themes를
 * placeholder로 채운다 — Part2에 새 계약을 요청해둔 상태다({@code editable}만 정확히 true).
 *
 * <p>{@code sourceAvailable}은 저장 시점엔 항상 true지만, 원본 Post가 이후 삭제·비공개
 * 전환되거나 작성자와 차단 관계가 되면 이 목록 조회 시점에 그걸 감지해서(현재로선 Post
 * 삭제·수정 API 자체가 아직 없어 사전 동기화 훅이 없음) {@code sourceAvailable=false}로
 * self-heal 처리하고 결과에서 제외한다. 한 페이지 안에서 이렇게 제외되는 항목이 있으면
 * 응답 items 수가 요청한 size보다 적을 수 있다 — 알려진 한계로 PR 리뷰 요청에 명시한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SavedRouteListService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;
    private static final List<PostVisibility> VISIBLE_TO_NON_AUTHOR =
            List.of(PostVisibility.PUBLIC, PostVisibility.MEMO_PRIVATE);
    private static final RegionSummary UNKNOWN_REGION = new RegionSummary("00000", "알수없음");

    private final SavedRouteRepository savedRouteRepository;
    private final PostRepository postRepository;
    private final TripPostReader tripPostReader;
    private final SocialRelationReader socialRelationReader;
    private final RegionReader regionReader;
    private final FileLinkService fileLinkService;
    private final PlaceReader placeReader;

    public SavedRouteListResponse list(Long cursorOrNull, Integer sizeOrNull, Long userId) {
        int size = clampSize(sizeOrNull);
        Pageable pageable = PageRequest.of(0, size + 1);

        List<SavedRoute> page = (cursorOrNull == null)
                ? savedRouteRepository.findFirstPage(userId, pageable)
                : findAfterCursor(cursorOrNull, userId, pageable);

        boolean hasNext = page.size() > size;
        List<SavedRoute> content = hasNext ? page.subList(0, size) : page;
        Long nextCursor = hasNext ? content.get(content.size() - 1).getSavedRouteId() : null;

        List<SavedRouteListItem> items = assembleItems(content);
        return new SavedRouteListResponse(items, nextCursor);
    }

    private List<SavedRoute> findAfterCursor(Long cursorId, Long userId, Pageable pageable) {
        SavedRoute cursor = savedRouteRepository.findByUserIdAndSavedRouteId(userId, cursorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CURSOR));
        return savedRouteRepository.findPageAfterCursor(
                userId, roundToStoredPrecision(cursor.getSavedAt()), cursorId, pageable);
    }

    private List<SavedRouteListItem> assembleItems(List<SavedRoute> routes) {
        List<SavedRoute> postRoutes = routes.stream()
                .filter(r -> r.getSourceType() == SavedRouteSourceType.POST)
                .toList();

        Map<Long, SavedRouteListItem> itemsByRouteId = new HashMap<>(resolvePostItems(postRoutes));

        for (SavedRoute route : routes) {
            if (route.getSourceType() == SavedRouteSourceType.AI_PREVIEW) {
                itemsByRouteId.put(route.getSavedRouteId(), aiPreviewPlaceholderItem(route));
            }
        }

        return routes.stream()
                .map(route -> itemsByRouteId.get(route.getSavedRouteId()))
                .filter(Objects::nonNull) // 접근 불가로 판정된 POST 항목은 여기서 자연스럽게 빠진다.
                .toList();
    }

    private Map<Long, SavedRouteListItem> resolvePostItems(List<SavedRoute> postRoutes) {
        if (postRoutes.isEmpty()) {
            return Map.of();
        }

        List<Long> postIds = postRoutes.stream().map(SavedRoute::getSourceId).distinct().toList();
        Map<Long, Post> postsById = new HashMap<>();
        postRepository.findAllById(postIds).forEach(post -> postsById.put(post.getPostId(), post));

        Map<Long, TripPostReader.TripPostSnapshot> tripsByPostId = new HashMap<>();
        List<Long> unavailableRouteIds = new ArrayList<>();
        for (SavedRoute route : postRoutes) {
            Post post = postsById.get(route.getSourceId());
            if (!isAccessible(post)) {
                unavailableRouteIds.add(route.getSavedRouteId());
                continue;
            }
            try {
                TripPostReader.TripPostSnapshot trip = tripPostReader.getTripForPost(post.getTripId());
                if (socialRelationReader.isBlockedEitherDirection(route.getUserId(), trip.ownerUserId())) {
                    unavailableRouteIds.add(route.getSavedRouteId());
                    continue;
                }
                tripsByPostId.put(post.getPostId(), trip);
            } catch (BusinessException e) {
                unavailableRouteIds.add(route.getSavedRouteId());
            }
        }
        if (!unavailableRouteIds.isEmpty()) {
            savedRouteRepository.markUnavailable(unavailableRouteIds);
        }

        Map<String, RegionSummary> regionsByCode = resolveRegions(tripsByPostId.values());
        Map<Long, String> thumbnailUrlsByFileId = resolveThumbnailUrls(postsById.values());
        Map<Long, List<String>> placeNamesByTripId = resolvePlaceNames(tripsByPostId.values());

        Map<Long, SavedRouteListItem> items = new HashMap<>();
        for (SavedRoute route : postRoutes) {
            TripPostReader.TripPostSnapshot trip = tripsByPostId.get(route.getSourceId());
            if (trip == null) {
                continue; // 접근 불가 판정된 항목
            }
            Post post = postsById.get(route.getSourceId());
            items.put(route.getSavedRouteId(), toPostItem(route, post, trip, regionsByCode, thumbnailUrlsByFileId, placeNamesByTripId));
        }
        return items;
    }

    private boolean isAccessible(Post post) {
        return post != null
                && post.getStatus() == PostStatus.PUBLISHED
                && VISIBLE_TO_NON_AUTHOR.contains(post.getVisibility())
                && post.getDeletedAt() == null;
    }

    private SavedRouteListItem toPostItem(SavedRoute route, Post post, TripPostReader.TripPostSnapshot trip,
                                           Map<String, RegionSummary> regionsByCode,
                                           Map<Long, String> thumbnailUrlsByFileId,
                                           Map<Long, List<String>> placeNamesByTripId) {
        RegionSummary region = regionsByCode.getOrDefault(trip.regionCode(), UNKNOWN_REGION);
        List<TravelTheme> themes = trip.themes().stream()
                .map(this::parseThemeOrNull)
                .filter(Objects::nonNull)
                .toList();
        return new SavedRouteListItem(
                route.getSavedRouteId(),
                SavedRouteSourceType.POST,
                route.getSourceId(),
                true,
                post.getTitle(),
                thumbnailUrlsByFileId.get(post.getRepresentativeFileId()),
                new AuthorSummary(trip.ownerUserId(), "", ""), // TODO(UserReader 실구현 merge되면 교체)
                region,
                placeNamesByTripId.getOrDefault(trip.tripId(), List.of()),
                themes,
                route.getSavedAt(),
                route.getUpdatedAt(),
                false // Post 기반 저장 경로는 항상 타인 소유(자신의 글은 저장 불가)라 editable=false 고정.
        );
    }

    private SavedRouteListItem aiPreviewPlaceholderItem(SavedRoute route) {
        return new SavedRouteListItem(
                route.getSavedRouteId(),
                SavedRouteSourceType.AI_PREVIEW,
                route.getSourceId(),
                true,
                "", // TODO(Part2 AiPreviewDisplayReader 계약 생기면 교체)
                null,
                null,
                UNKNOWN_REGION,
                List.of(),
                List.of(),
                route.getSavedAt(),
                route.getUpdatedAt(),
                true // AI_PREVIEW는 본인 소유 Preview만 저장 가능하므로 항상 editable.
        );
    }

    private TravelTheme parseThemeOrNull(String raw) {
        try {
            return TravelTheme.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Map<String, RegionSummary> resolveRegions(java.util.Collection<TripPostReader.TripPostSnapshot> trips) {
        List<String> regionCodes = trips.stream()
                .map(TripPostReader.TripPostSnapshot::regionCode)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (regionCodes.isEmpty()) {
            return Map.of();
        }
        Map<String, RegionSummary> regionsByCode = new HashMap<>();
        for (RegionSummary region : regionReader.getRegions(regionCodes)) {
            regionsByCode.put(region.sigunguCode(), region);
        }
        return regionsByCode;
    }

    private Map<Long, String> resolveThumbnailUrls(java.util.Collection<Post> posts) {
        List<Long> fileIds = posts.stream()
                .map(Post::getRepresentativeFileId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, String> urlsByFileId = new HashMap<>();
        for (ImageSummary summary : fileLinkService.getImageUrls(fileIds)) {
            urlsByFileId.put(summary.fileId(), summary.imageUrl());
        }
        return urlsByFileId;
    }

    private Map<Long, List<String>> resolvePlaceNames(java.util.Collection<TripPostReader.TripPostSnapshot> trips) {
        Map<Long, List<TripPostReader.TripPostPlaceSnapshot>> placesByTripId = new HashMap<>();
        for (TripPostReader.TripPostSnapshot trip : trips) {
            placesByTripId.put(trip.tripId(), tripPostReader.getTripPlaceSnapshot(trip.tripId()));
        }

        List<Long> allPlaceIds = placesByTripId.values().stream()
                .flatMap(List::stream)
                .map(TripPostReader.TripPostPlaceSnapshot::placeId)
                .distinct()
                .toList();
        Map<Long, String> namesByPlaceId = new HashMap<>();
        if (!allPlaceIds.isEmpty()) {
            for (PlaceContract place : placeReader.getPlaces(allPlaceIds, null)) {
                namesByPlaceId.put(place.placeId(), place.name());
            }
        }

        Map<Long, List<String>> placeNamesByTripId = new HashMap<>();
        for (Map.Entry<Long, List<TripPostReader.TripPostPlaceSnapshot>> entry : placesByTripId.entrySet()) {
            List<String> names = entry.getValue().stream()
                    .sorted(Comparator.comparingInt(TripPostReader.TripPostPlaceSnapshot::dayNumber)
                            .thenComparingInt(TripPostReader.TripPostPlaceSnapshot::visitOrder))
                    .map(place -> namesByPlaceId.get(place.placeId()))
                    .filter(Objects::nonNull)
                    .toList();
            placeNamesByTripId.put(entry.getKey(), names);
        }
        return placeNamesByTripId;
    }

    /** {@code SavedRoute} Javadoc 참고 — 같은 트랜잭션 안에서 만든 행의 반올림 전 값을 보정한다. */
    private LocalDateTime roundToStoredPrecision(LocalDateTime value) {
        LocalDateTime truncated = value.truncatedTo(ChronoUnit.SECONDS);
        return value.getNano() >= 500_000_000 ? truncated.plusSeconds(1) : truncated;
    }

    private int clampSize(Integer sizeOrNull) {
        if (sizeOrNull == null) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(Math.max(sizeOrNull, 1), MAX_PAGE_SIZE);
    }
}
