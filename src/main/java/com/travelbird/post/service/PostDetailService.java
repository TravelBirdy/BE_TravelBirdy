package com.travelbird.post.service;

import com.travelbird.common.dto.ImageSummary;
import com.travelbird.file.api.FileLinkService;
import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.place.api.PlaceContract;
import com.travelbird.place.api.PlaceReader;
import com.travelbird.post.api.AuthorSummary;
import com.travelbird.post.controller.dto.PostDetailPlaceResponse;
import com.travelbird.post.controller.dto.PostDetailResponse;
import com.travelbird.post.controller.dto.PostDetailRouteDay;
import com.travelbird.post.controller.dto.PostRouteCoordinates;
import com.travelbird.post.domain.Post;
import com.travelbird.post.domain.PostImage;
import com.travelbird.post.domain.PostPlace;
import com.travelbird.post.domain.PostStatus;
import com.travelbird.post.domain.PostVisibility;
import com.travelbird.post.repository.PostImageRepository;
import com.travelbird.post.repository.PostPlaceRepository;
import com.travelbird.post.repository.PostRepository;
import com.travelbird.trip.api.TripPostReader;
import com.travelbird.user.api.UserReader;
import com.travelbird.user.api.UserSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 게시글 상세. backend-functional-spec-v10.md §3.8.3. 비로그인도 허용한다(SecurityConfig
 * permitAll, PR#17) — 비작성자는 {@code PUBLISHED}+{@code PUBLIC|MEMO_PRIVATE}만 볼 수
 * 있고, 그 외(PRIVATE/BLOCKED/삭제/존재하지 않음)는 전부 {@code 404 POST_NOT_FOUND}로
 * 통일한다. {@code MEMO_PRIVATE} 비작성자는 장소 메모만 마스킹하고 사진은 그대로
 * 공개한다(§3.8.2) — {@code trip.mapper.TripMapper.detail()}과 동일한 mask idiom.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostDetailService {

    private static final List<PostVisibility> VISIBLE_TO_NON_AUTHOR =
            List.of(PostVisibility.PUBLIC, PostVisibility.MEMO_PRIVATE);

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final PostPlaceRepository postPlaceRepository;
    private final TripPostReader tripPostReader;
    private final PlaceReader placeReader;
    private final FileLinkService fileLinkService;
    private final UserReader userReader;

    public PostDetailResponse getDetail(Long postId, Long viewerIdOrNull) {
        Post post = postRepository.findByPostIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        TripPostReader.TripPostSnapshot trip;
        try {
            trip = tripPostReader.getTripForPost(post.getTripId());
        } catch (BusinessException e) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        boolean owner = viewerIdOrNull != null && viewerIdOrNull.equals(trip.ownerUserId());
        if (!owner) {
            boolean accessible = post.getStatus() == PostStatus.PUBLISHED
                    && VISIBLE_TO_NON_AUTHOR.contains(post.getVisibility());
            if (!accessible) {
                throw new BusinessException(ErrorCode.POST_NOT_FOUND);
            }
        }
        boolean mask = !owner && post.getVisibility() == PostVisibility.MEMO_PRIVATE;

        AuthorSummary author = resolveAuthor(trip.ownerUserId());
        List<ImageSummary> images = resolvePostImages(post);

        List<TripPostReader.TripPostPlaceSnapshot> placeSnapshots = resolveAttachedPlaces(post, trip);
        Map<Long, PlaceContract> placesById = resolvePlaces(placeSnapshots);

        List<PostDetailPlaceResponse> places = placeSnapshots.stream()
                .sorted(Comparator.comparingInt(TripPostReader.TripPostPlaceSnapshot::dayNumber)
                        .thenComparingInt(TripPostReader.TripPostPlaceSnapshot::visitOrder))
                .map(snapshot -> toPlaceResponse(snapshot, placesById.get(snapshot.placeId()), mask))
                .toList();
        List<PostDetailRouteDay> route = buildRoute(placeSnapshots, placesById);

        return new PostDetailResponse(
                author,
                post.getTitle(),
                post.getContent(),
                images,
                places,
                route,
                post.getVisibility(),
                post.getViewCount(),
                post.getSaveCount(),
                post.getShareCount());
    }

    private AuthorSummary resolveAuthor(Long ownerUserId) {
        try {
            UserSummary summary = userReader.getUserSummary(ownerUserId);
            return new AuthorSummary(ownerUserId, summary.nickname(),
                    summary.birdType() == null ? null : summary.birdType().name());
        } catch (BusinessException e) {
            return new AuthorSummary(ownerUserId, null, null);
        }
    }

    /** {@code getImageUrls}는 순서를 보장하지 않아서 {@code displayOrder}대로 재배열한다. */
    private List<ImageSummary> resolvePostImages(Post post) {
        List<PostImage> images = postImageRepository.findByIdPostIdOrderByDisplayOrderAsc(post.getPostId());
        return orderedImageSummaries(images.stream().map(image -> image.getId().getFileId()).toList());
    }

    private List<ImageSummary> orderedImageSummaries(List<Long> orderedFileIds) {
        if (orderedFileIds.isEmpty()) {
            return List.of();
        }
        Map<Long, String> urlsByFileId = new HashMap<>();
        for (ImageSummary summary : fileLinkService.getImageUrls(orderedFileIds)) {
            urlsByFileId.put(summary.fileId(), summary.imageUrl());
        }
        List<ImageSummary> ordered = new ArrayList<>();
        for (Long fileId : orderedFileIds) {
            String url = urlsByFileId.get(fileId);
            if (url != null) {
                ordered.add(new ImageSummary(fileId, url));
            }
        }
        return ordered;
    }

    /** 이 게시글에 실제로 걸린 tripPlaceId만 트립 전체 스냅샷에서 필터링한다. */
    private List<TripPostReader.TripPostPlaceSnapshot> resolveAttachedPlaces(
            Post post, TripPostReader.TripPostSnapshot trip) {
        Set<Long> attachedTripPlaceIds = postPlaceRepository.findByIdPostId(post.getPostId()).stream()
                .map(postPlace -> postPlace.getId().getTripPlaceId())
                .collect(java.util.stream.Collectors.toSet());
        if (attachedTripPlaceIds.isEmpty()) {
            return List.of();
        }
        return trip.days().stream()
                .flatMap(day -> day.places().stream())
                .filter(place -> attachedTripPlaceIds.contains(place.tripPlaceId()))
                .toList();
    }

    private Map<Long, PlaceContract> resolvePlaces(List<TripPostReader.TripPostPlaceSnapshot> placeSnapshots) {
        List<Long> placeIds = placeSnapshots.stream()
                .map(TripPostReader.TripPostPlaceSnapshot::placeId)
                .distinct()
                .toList();
        if (placeIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, PlaceContract> placesById = new HashMap<>();
        for (PlaceContract place : placeReader.getPlaces(placeIds, null)) {
            placesById.put(place.placeId(), place);
        }
        return placesById;
    }

    private PostDetailPlaceResponse toPlaceResponse(TripPostReader.TripPostPlaceSnapshot snapshot,
                                                      PlaceContract placeOrNull, boolean mask) {
        List<ImageSummary> placeImages = orderedImageSummaries(snapshot.imageFileIds());
        return new PostDetailPlaceResponse(
                snapshot.placeId(),
                placeOrNull == null ? null : placeOrNull.name(),
                placeOrNull == null ? null : placeOrNull.category(),
                placeOrNull == null ? null : placeOrNull.address(),
                placeOrNull == null ? null : placeOrNull.latitude(),
                placeOrNull == null ? null : placeOrNull.longitude(),
                snapshot.dayNumber(),
                snapshot.visitOrder(),
                mask ? null : snapshot.memo(),
                mask,
                placeImages);
    }

    /** Day별로 그룹핑하고 방문 순서대로 좌표만 뽑는다. */
    private List<PostDetailRouteDay> buildRoute(List<TripPostReader.TripPostPlaceSnapshot> placeSnapshots,
                                                  Map<Long, PlaceContract> placesById) {
        Map<Integer, List<TripPostReader.TripPostPlaceSnapshot>> byDay = new TreeMap<>();
        for (TripPostReader.TripPostPlaceSnapshot snapshot : placeSnapshots) {
            byDay.computeIfAbsent(snapshot.dayNumber(), d -> new ArrayList<>()).add(snapshot);
        }
        List<PostDetailRouteDay> route = new ArrayList<>();
        for (Map.Entry<Integer, List<TripPostReader.TripPostPlaceSnapshot>> entry : byDay.entrySet()) {
            List<PostRouteCoordinates> points = entry.getValue().stream()
                    .sorted(Comparator.comparingInt(TripPostReader.TripPostPlaceSnapshot::visitOrder))
                    .map(snapshot -> placesById.get(snapshot.placeId()))
                    .filter(java.util.Objects::nonNull)
                    .map(place -> new PostRouteCoordinates(place.latitude(), place.longitude()))
                    .toList();
            route.add(new PostDetailRouteDay(entry.getKey(), points));
        }
        return route;
    }
}
